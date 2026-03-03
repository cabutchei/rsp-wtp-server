package com.github.cabutchei.rsp.eclipse.wst.publishing;



import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;

import com.github.cabutchei.rsp.api.DefaultServerAttributes;
import com.github.cabutchei.rsp.api.ServerManagementAPIConstants;
import com.github.cabutchei.rsp.api.dao.DeployableReference;
import com.github.cabutchei.rsp.api.dao.DeployableState;
import com.github.cabutchei.rsp.eclipse.core.runtime.IStatus;
import com.github.cabutchei.rsp.eclipse.core.runtime.Status;
import com.github.cabutchei.rsp.server.ServerCoreActivator;
import com.github.cabutchei.rsp.server.model.AbstractServerDelegate;
import com.github.cabutchei.rsp.server.model.internal.publishing.DeployableDelta;
import com.github.cabutchei.rsp.server.model.internal.publishing.DeploymentAssemblyDiscovery;
import com.github.cabutchei.rsp.server.model.internal.publishing.DeploymentAssemblyDiscovery.IDeploymentAssembler;
import com.github.cabutchei.rsp.server.model.internal.publishing.DeploymentAssemblyFile;
import com.github.cabutchei.rsp.server.model.internal.publishing.DeploymentAssemblyMapping;
import com.github.cabutchei.rsp.server.spi.filewatcher.IFileWatcherService;
import com.github.cabutchei.rsp.server.spi.filewatcher.FileWatcherEvent;
import com.github.cabutchei.rsp.server.spi.filewatcher.IFileWatcherEventListener;
import com.github.cabutchei.rsp.server.spi.publishing.IFullPublishRequiredCallback;
import com.github.cabutchei.rsp.server.spi.model.IServerModel;
import com.github.cabutchei.rsp.server.spi.model.IServerModelListener;
import com.github.cabutchei.rsp.server.spi.model.ServerModelListenerAdapter;
import com.github.cabutchei.rsp.server.spi.servertype.IDeployableDelta;
import com.github.cabutchei.rsp.server.spi.servertype.IDeploymentAssemblyMapping;
import com.github.cabutchei.rsp.server.spi.servertype.IServerListener;
import com.github.cabutchei.rsp.server.spi.servertype.IServerPublishModel;
import com.github.cabutchei.rsp.server.spi.servertype.ServerEvent;
import com.github.cabutchei.rsp.eclipse.osgi.util.NLS;

import com.github.cabutchei.rsp.eclipse.wst.api.IWstServerControl;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.Path;
import java.nio.file.Paths;



public class WSTServerPublishStateModel implements IServerPublishModel, IFileWatcherEventListener {

	static final Logger LOG = LoggerFactory.getLogger(WSTServerPublishStateModel.class);
	
	private final Map<String, DeployableState> states;
	private final Map<String, Map<String,Object>> deploymentOptions;
	private final Map<String, DeployableDelta> deltas = new HashMap<>();
	private final Map<String, DeploymentAssemblyFile> assembly = new HashMap<>();
	private static final long WORKSPACE_REFRESH_DEBOUNCE_MS = 300;
	private final Object refreshLock = new Object();
	private final Map<String, PendingWorkspaceRefresh> pendingWorkspaceRefreshes = new HashMap<>();
	private long refreshSequence = 0;
	
	private final AbstractServerDelegate delegate;
    private IWstServerControl wstServerControl;
    private final Supplier<IWstServerControl> controlSupplier;
	private final IFileWatcherService fileWatcher;
	private int publishState = AbstractServerDelegate.PUBLISH_STATE_UNKNOWN;
	
	private WSTAutoPublishThread autoPublish;
	private boolean autoPublishListenerRegistered;
	private boolean publishStateListenerRegistered;
	private final IServerModelListener autoPublishListener = new ServerModelListenerAdapter() {
		@Override
		public void serverAttributesChanged(com.github.cabutchei.rsp.api.dao.ServerHandle server) {
			if (server == null || delegate == null || delegate.getServerHandle() == null) {
				return;
			}
			if (server.getId().equals(delegate.getServerHandle().getId())) {
				handleAutoPublishConfigChanged();
			}
		}
	};
	private final IServerListener publishStateListener = new IServerListener() {
		@Override
		public void serverChanged(ServerEvent event) {
			if (event == null) {
				return;
			}
			int kind = event.getKind();
			if ((kind & ServerEvent.PUBLISH_STATE_CHANGE) != 0
					|| (kind & ServerEvent.MODULE_CHANGE) != 0) {
				boolean deployableStatesChanged = synchronizeStatesFromControl();
				int previousPublishState = getServerPublishState();
				updateServerPublishStateFromDeployments(true);
				if (deployableStatesChanged && previousPublishState == getServerPublishState()) {
					fireState();
				}
			}
			if ((kind & ServerEvent.PUBLISH_STATE_CHANGE) == 0 || !isAutoPublisherEnabled()) {
				return;
			}
			if (event.getPublishState() != ServerManagementAPIConstants.PUBLISH_STATE_NONE) {
				launchOrUpdateAutopublishThread();
			}
		}
	};

	private IFullPublishRequiredCallback fullPublishRequired;
	
	public WSTServerPublishStateModel(AbstractServerDelegate delegate, Supplier<IWstServerControl> controlSupplier, IFileWatcherService fileWatcher) {
		this(delegate, controlSupplier, fileWatcher, null);
	}
	public WSTServerPublishStateModel(AbstractServerDelegate delegate, Supplier<IWstServerControl> controlSupplier,
			IFileWatcherService fileWatcher, IFullPublishRequiredCallback fullPublishRequired) {
		this.delegate = delegate;
        this.controlSupplier = controlSupplier == null ? () -> null : controlSupplier;
		this.wstServerControl = this.controlSupplier.get();
		this.fileWatcher = fileWatcher;
		this.fullPublishRequired = fullPublishRequired;
		this.states = new LinkedHashMap<>();
		this.deploymentOptions = new LinkedHashMap<>();
	}

	public WSTServerPublishStateModel(AbstractServerDelegate delegate, IWstServerControl wstServerControl,
			IFileWatcherService fileWatcher, IFullPublishRequiredCallback fullPublishRequired) {
		this.delegate = delegate;
		this.wstServerControl = wstServerControl;
		this.controlSupplier = () -> this.wstServerControl;
		this.fileWatcher = fileWatcher;
		this.fullPublishRequired = fullPublishRequired;
		this.states = new LinkedHashMap<>();
		this.deploymentOptions = new LinkedHashMap<>();
	}

	@Override
	public synchronized void initialize(List<DeployableReference> references) {
		if (references != null) {
			for (DeployableReference reference : references) {
				if (reference != null) {
					deploymentOptions.put(getKey(reference), reference.getOptions());
				}
			}
		}

		synchronizeStatesFromControl();
		if (states.isEmpty() && references != null && !references.isEmpty()) {
			for (DeployableReference reference : references) {
				addMissingState(reference);
			}
		}
		updateServerPublishStateFromDeployments();
		fireState();
		registerAutoPublishConfigListener();
		registerPublishStateListener();
	}

	private IWstServerControl resolveWstServerControl() {
		if (wstServerControl == null && controlSupplier != null) {
			wstServerControl = controlSupplier.get();
		}
		return wstServerControl;
	}

	private boolean synchronizeStatesFromControl() {
		IWstServerControl control = resolveWstServerControl();
		if (control == null) {
			return false;
		}
		List<DeployableState> fromControl = control.getDeployableStates();
		Map<String, DeployableState> next = new LinkedHashMap<>();
		if (fromControl != null) {
			for (DeployableState state : fromControl) {
				if (state == null || state.getReference() == null) {
					continue;
				}
				DeployableReference reference = state.getReference();
				String key = getKey(reference);
				next.put(key, cloneDeployableState(reference, state));
			}
		}
		boolean changed = !sameDeployableStates(states, next);

		List<DeployableReference> removed = new ArrayList<>();
		for (Map.Entry<String, DeployableState> entry : states.entrySet()) {
			if (!next.containsKey(entry.getKey()) && entry.getValue() != null) {
				removed.add(entry.getValue().getReference());
			}
		}
		for (DeployableReference reference : removed) {
			unregisterFileWatcher(reference);
			deployableRemoved(reference);
		}
		for (Map.Entry<String, DeployableState> entry : next.entrySet()) {
			if (!states.containsKey(entry.getKey())) {
				IStatus status = registerDeployable(entry.getValue().getReference());
				if (!status.isOK()) {
					LOG.warn("Failed to register deployable {}", entry.getKey());
				}
			}
		}
		states.clear();
		states.putAll(next);
		return changed;
	}

	private boolean sameDeployableStates(Map<String, DeployableState> current, Map<String, DeployableState> next) {
		if (current == next) {
			return true;
		}
		if (current == null || next == null || current.size() != next.size()) {
			return false;
		}
		for (Map.Entry<String, DeployableState> entry : current.entrySet()) {
			DeployableState nextState = next.get(entry.getKey());
			if (!sameDeployableState(entry.getValue(), nextState)) {
				return false;
			}
		}
		return true;
	}

	private boolean sameDeployableState(DeployableState left, DeployableState right) {
		if (left == right) {
			return true;
		}
		if (left == null || right == null) {
			return false;
		}
		if (left.getState() != right.getState()) {
			return false;
		}
		if (left.getPublishState() != right.getPublishState()) {
			return false;
		}
		DeployableReference leftRef = left.getReference();
		DeployableReference rightRef = right.getReference();
		if (leftRef == rightRef) {
			return true;
		}
		if (leftRef == null || rightRef == null) {
			return false;
		}
		return leftRef.equals(rightRef);
	}

	private void addMissingState(DeployableReference reference) {
		if (reference == null) {
			return;
		}
		String key = getKey(reference);
		if (states.containsKey(key)) {
			return;
		}
		DeployableState state = createDeployableState(reference, ServerManagementAPIConstants.PUBLISH_STATE_UNKNOWN,
				ServerManagementAPIConstants.STATE_UNKNOWN);
		states.put(key, state);
		IStatus status = registerDeployable(reference);
		if (!status.isOK()) {
			LOG.warn("Failed to register deployable {}", key);
		}
	}

	private DeployableState cloneDeployableState(DeployableReference reference, DeployableState state) {
		return createDeployableState(reference, state.getPublishState(), state.getState());
	}
	
	private DeployableState createDeployableState(DeployableReference reference, int publishState, int state) {
		DeployableState deployableState = new DeployableState();
		deployableState.setPublishState(publishState);
		deployableState.setState(state);
		deployableState.setReference(new DeployableReference(reference));
		deployableState.setServer(delegate.getServerHandle());
		return deployableState;
	}
	
	private IStatus cacheAssemblies(DeployableReference reference) {
		IDeploymentAssembler[] assemblers = DeploymentAssemblyDiscovery.getAssemblers(reference);
		for( int i = 0; i < assemblers.length; i++ ) {
			try {
				DeploymentAssemblyFile file = assemblers[i].getAssemblerFile(reference);
				if( file != null ) {
					assembly.put(getKey(reference), file);
					return Status.OK_STATUS;
				}
			} catch( IOException ioe) {
				return new Status(IStatus.ERROR, ServerCoreActivator.BUNDLE_ID, IStatus.ERROR, 
						NLS.bind("Could not add deployable with path {0}: Error configuring assembly / packaging", 
								getKey(reference)), ioe);
			}
		}
		return Status.OK_STATUS;
	}
	
	private IStatus registerFileWatcher(DeployableReference reference) {
		if( fileWatcher != null ) {
			// DEPLOY_ASSEMBLY
			IStatus ret = cacheAssemblies(reference);
			if( !ret.isOK()) {
				return ret;
			}
			
			List<Path> sourcePaths = getDeploySourceFolders(reference);
			for( int i = 0; i < sourcePaths.size(); i++ ) {
				Path sourcePathToWatch = sourcePaths.get(i);
				File asFile = sourcePathToWatch.toFile();
				if( asFile.exists()) {
					boolean recursive = asFile.exists() && asFile.isDirectory();
					//System.out.println("  Source to watch: " + sourcePathToWatch.toString());
					fileWatcher.addFileWatcherListener(sourcePathToWatch, this, recursive);
				}
			}
		}
		return Status.OK_STATUS;
	}
	
	private List<Path> getDeploySourceFolders(DeployableReference ref) {
		DeploymentAssemblyFile assemblyData = assembly.get(getKey(ref));
		if( assemblyData == null ) {
			File f = new File(ref.getPath());
			ArrayList<Path> al = new ArrayList<Path>();
			al.add(f.toPath());
			return al;
		} else {
			return getAssemblySourceFolders(ref);
		}
	}
	private List<Path> getAssemblySourceFolders(DeployableReference ref) {
		List<Path> ret = new ArrayList<Path>();
		DeploymentAssemblyFile assemblyData = assembly.get(getKey(ref));
		IDeploymentAssemblyMapping[] mappings = assemblyData.getMappings();
		if( mappings != null ) {
			for( int i = 0; i < mappings.length; i++ ) {
				IDeploymentAssemblyMapping singleMapping = mappings[i];
				String source = singleMapping.getSource();
				Path sourcePathToWatch = Paths.get(ref.getPath(), source);
				File sourcePathFileToWatch = sourcePathToWatch.toFile();
				if( sourcePathFileToWatch.exists()) {
					ret.add(sourcePathToWatch);
				}
			}
		}
		return ret;
	}

	/**
	 * Adds the given deployable to this model.
	 * 
	 * @param withOptions the deployable to add.
	 */
	@Override
	public synchronized IStatus addDeployable(DeployableReference withOptions) {
		if (contains(withOptions)) {
			return new Status(IStatus.ERROR, ServerCoreActivator.BUNDLE_ID, IStatus.ERROR,
					NLS.bind("Could not add deployable with path {0}: it already exists.", getKey(withOptions)),
					null);
		}

		IStatus status = addDeployableImpl(withOptions);
		if (!status.isOK()) {
			return status;
		}
		updateServerPublishStateFromDeployments();
		fireState();
		launchOrUpdateAutopublishThread();
		return Status.OK_STATUS;
	}

	private IStatus addDeployableImpl(DeployableReference reference) {
		IWstServerControl control = resolveWstServerControl();
		if (control == null) {
			return new Status(IStatus.ERROR, ServerCoreActivator.BUNDLE_ID, IStatus.ERROR,
					"WST server control is not available", null);
		}
		IStatus status = control.addDeployable(reference);
		if (!status.isOK()) {
			return status;
		}
		synchronizeStatesFromControl();
		addMissingState(reference);
		return Status.OK_STATUS;
	}

	@Override
	public synchronized boolean contains(DeployableReference reference) {
		if (reference == null) {
			return false;
		}
		return states.containsKey(getKey(reference));
	}

	@Override
	public synchronized IStatus removeDeployable(DeployableReference reference) {
		if (!contains(reference)) {
			return new Status(IStatus.ERROR, ServerCoreActivator.BUNDLE_ID, IStatus.ERROR, 
					NLS.bind("Could not remove deploybale with path {0}: it doesn't exist", getKey(reference)),
							null);
		}
		IWstServerControl control = resolveWstServerControl();
		if (control == null) {
			return new Status(IStatus.ERROR, ServerCoreActivator.BUNDLE_ID, IStatus.ERROR,
					"WST server control is not available", null);
		}
		IStatus status = control.removeDeployable(reference);
		if (!status.isOK()) {
			return status;
		}
		synchronizeStatesFromControl();
		if (contains(reference)) {
			unregisterFileWatcher(reference);
			deployableRemoved(reference);
		}
		updateServerPublishStateFromDeployments();
		fireState();
		launchOrUpdateAutopublishThread();
		return Status.OK_STATUS;
	}

	protected String getKey(DeployableReference reference) {
		if (reference == null) {
			return null;
		} 
		return reference.getPath();
	}

	private void unregisterFileWatcher(DeployableReference reference) {
		if (fileWatcher == null || reference == null) {
			return;
		}
		List<Path> sourcePaths = getDeploySourceFolders(reference);
		for (int i = 0; i < sourcePaths.size(); i++) {
			Path sourcePathToWatch = sourcePaths.get(i);
			fileWatcher.removeFileWatcherListener(sourcePathToWatch, this);
		}
	}

	private void scheduleWorkspaceRefresh(FileWatcherEvent event, Path affected) {
		WorkspaceRefreshTarget target = createWorkspaceRefreshTarget(event, affected);
		if (target == null || target.getProject() == null || !target.getProject().exists()) {
			return;
		}
		String projectKey = target.getProject().getName();
		final long scheduledSequence;
		synchronized (refreshLock) {
			PendingWorkspaceRefresh previous = pendingWorkspaceRefreshes.get(projectKey);
			WorkspaceRefreshTarget merged = mergeRefreshTargets(previous == null ? null : previous.getTarget(), target);
			scheduledSequence = ++refreshSequence;
			pendingWorkspaceRefreshes.put(projectKey, new PendingWorkspaceRefresh(scheduledSequence, merged));
		}

		CompletableFuture.runAsync(() -> runWorkspaceRefresh(projectKey, scheduledSequence),
				CompletableFuture.delayedExecutor(WORKSPACE_REFRESH_DEBOUNCE_MS, TimeUnit.MILLISECONDS));
	}

	private void runWorkspaceRefresh(String projectKey, long scheduledSequence) {
		WorkspaceRefreshTarget target = null;
		synchronized (refreshLock) {
			PendingWorkspaceRefresh pending = pendingWorkspaceRefreshes.get(projectKey);
			if (pending == null || pending.getSequence() != scheduledSequence) {
				return;
			}
			pendingWorkspaceRefreshes.remove(projectKey);
			target = pending.getTarget();
		}
		if (target == null) {
			return;
		}
		IProject project = target.getProject();
		if (project == null || !project.exists()) {
			return;
		}
		IResource resource = target.getResource();
		int depth = target.getDepth();
		try {
			if (resource != null && resource.exists()) {
				resource.refreshLocal(depth, null);
			} else {
				project.refreshLocal(IResource.DEPTH_ONE, null);
			}
		} catch (org.eclipse.core.runtime.CoreException e) {
			LOG.warn("Failed to refresh workspace for {}", target.getPath(), e);
		}
	}

	private WorkspaceRefreshTarget mergeRefreshTargets(WorkspaceRefreshTarget previous, WorkspaceRefreshTarget next) {
		if (previous == null) {
			return next;
		}
		if (next == null) {
			return previous;
		}
		if (previous.getResource() != null && previous.getResource().equals(next.getResource())) {
			int depth = Math.max(previous.getDepth(), next.getDepth());
			return new WorkspaceRefreshTarget(next.getPath(), next.getProject(), next.getResource(), depth);
		}
		return new WorkspaceRefreshTarget(next.getPath(), next.getProject(), null, IResource.DEPTH_ONE);
	}

	private WorkspaceRefreshTarget createWorkspaceRefreshTarget(FileWatcherEvent event, Path affected) {
		if (affected == null) {
			return null;
		}
		WorkspaceResolution resolution = resolveWorkspaceResolution(affected);
		IProject project = resolution == null ? null : resolution.getProject();
		if (project == null) {
			return null;
		}

		IResource resource = resolution.getResource();
		WatchEvent.Kind<?> kind = event == null ? null : event.getKind();
		if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
			if (resource != null && resource.exists() && resource.getType() == IResource.FILE) {
				return new WorkspaceRefreshTarget(affected, project, resource, IResource.DEPTH_ZERO);
			}
			if (resource != null && resource.exists()) {
				return new WorkspaceRefreshTarget(affected, project, resource, IResource.DEPTH_ONE);
			}
			return new WorkspaceRefreshTarget(affected, project, null, IResource.DEPTH_ONE);
		}

		if (resource != null) {
			if (resource.exists() && resource.getType() == IResource.FILE) {
				IContainer parent = resource.getParent();
				if (parent != null) {
					return new WorkspaceRefreshTarget(affected, project, parent, IResource.DEPTH_ONE);
				}
			}
			if (resource.exists()) {
				return new WorkspaceRefreshTarget(affected, project, resource, IResource.DEPTH_ONE);
			}
			IContainer parent = resource.getParent();
			if (parent != null && parent.exists()) {
				return new WorkspaceRefreshTarget(affected, project, parent, IResource.DEPTH_ONE);
			}
		}

		return new WorkspaceRefreshTarget(affected, project, null, IResource.DEPTH_ONE);
	}

	private WorkspaceResolution resolveWorkspaceResolution(Path path) {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		org.eclipse.core.runtime.Path eclipsePath = new org.eclipse.core.runtime.Path(path.toString());
		IResource resource = root.getFileForLocation(eclipsePath);
		if (resource == null) {
			resource = root.getContainerForLocation(eclipsePath);
		}
		if (resource == null) {
			IFile[] files = root.findFilesForLocationURI(path.toUri());
			if (files != null && files.length > 0) {
				resource = files[0];
			}
		}
		if (resource == null) {
			IContainer[] containers = root.findContainersForLocationURI(path.toUri());
			if (containers != null && containers.length > 0) {
				resource = containers[0];
			}
		}
		if (resource != null) {
			return new WorkspaceResolution(resource.getProject(), resource);
		}
		java.nio.file.Path normalized = path.toAbsolutePath().normalize();
		IProject bestMatch = null;
		int bestSegments = -1;
		for (IProject project : root.getProjects()) {
			org.eclipse.core.runtime.IPath location = project.getLocation();
			if (location == null) {
				continue;
			}
			java.nio.file.Path projectLocation = location.toFile().toPath().toAbsolutePath().normalize();
			if (!normalized.startsWith(projectLocation)) {
				continue;
			}
			int segments = projectLocation.getNameCount();
			if (segments > bestSegments) {
				bestMatch = project;
				bestSegments = segments;
			}
		}
		return new WorkspaceResolution(bestMatch, null);
	}
	
	@Override
	public synchronized void deployableRemoved(DeployableReference reference) {
		String k = getKey(reference);
		states.remove(k);
		deploymentOptions.remove(k);
		deltas.remove(k);
		assembly.remove(k);
	}

	@Override
	public synchronized List<DeployableState> getDeployableStates() {
		List<DeployableState> ret = new ArrayList<>(states.size());
		for (DeployableState element : states.values()) {
			DeployableState state = cloneDeployableState(element.getReference(), element);
			if (state != null && state.getReference() != null) {
				fillOptionsFromCache(state.getReference());
			}
			ret.add(state);
		}
		return ret;
	}

	@Override
	public synchronized List<DeployableState> getDeployableStatesWithOptions() {
		List<DeployableState> ret = new ArrayList<>(states.size());
		for (DeployableState element : states.values()) {
			DeployableState state = cloneDeployableState(element.getReference(), element);
			if (state != null && state.getReference() != null) {
				fillOptionsFromCache(state.getReference());
			}
			ret.add(state);
		}
		return ret;
	}

	@Override
	public synchronized DeployableState getDeployableState(DeployableReference reference) {
		if (reference == null) {
			return null;
		}
		DeployableState state = states.get(getKey(reference));
		if (state != null && state.getReference() != null) {
			DeployableState clone = cloneDeployableState(state.getReference(), state);
			fillOptionsFromCache(clone.getReference());
			return clone;
		}
		return null;
	}

	/**
	 * for testing purposes
	 */
	protected Map<String, DeployableState> getStates() {
		return states;
	}

	/**
	 * for testing purposes
	 */
	protected Map<String, DeployableDelta> getDeltas() {
		return deltas;
	}

	public IDeploymentAssemblyMapping[] getDeployableResourceMappings(DeployableReference reference) {
		DeploymentAssemblyFile file = assembly.get(getKey(reference));
		if( file != null ) {
			return file.getMappings();
		}
		return new IDeploymentAssemblyMapping[] {
				new DeploymentAssemblyMapping(reference.getPath(), "/")
		};
	}

	
	@Override
	public synchronized void setDeployablePublishState(DeployableReference reference, int publishState) {
		DeployableState current = getDeployableState(reference);
		if (current == null) {
			return;
		}
		String key = getKey(reference);
		DeployableState next = createDeployableState(reference, publishState, current.getState());
		states.put(key, next);
		if (publishState == ServerManagementAPIConstants.PUBLISH_STATE_NONE) {
			clearDelta(key);
		}
		updateServerPublishStateFromDeployments();
		launchOrUpdateAutopublishThread();

	}

	private void clearDelta(String key) {
		DeployableDelta delta2 = getDeltas().get(key);
		if( delta2 != null ) {
			delta2.clear();
		}
	}

	@Override
	public synchronized void setDeployableState(DeployableReference reference, int runState) {
		DeployableState ds = states.get(getKey(reference));
		if (ds == null) {
			return;
		}
		DeployableState next = createDeployableState(reference, ds.getPublishState(), runState);
		states.put(getKey(reference), next);
	}

	/*
	 * If a path matching one of our deployments has been modified,
	 * created, or deleted, we should respond to this. However, most 
	 * cases will not require us to do much of anything. 
	 * 
	 *  If the deployment is currently set to be 'added', we should not make any changes
	 *  to the publish state, since the next publish will do the full add as expected.
	 *  
	 *  If the deployment is currently set to be 'removed', we should not make any 
	 *  changes to the publish state, since the next publish will remove the deployment.
	 *  
	 *  If the deployment is currently set to be 'incremental', 
	 *  no change is needed. It's already marked as requiring a publish. 
	 *  
	 *  If the deployment is currently set to be 'full',
	 *  no change is needed. It's already marked as requiring a publish.
	 *  However it's worth registering the delta in case a server delegate 
	 *  needs to know the list of changed resources.
	 *  
	 *  If the deployment is currently set to 'unknown', 
	 *  we should not make any change, so the delegate knows the state is still uncertain.
	 *  
	 *  So only if the deployment is currently set to 'none' do we know 
	 *  that we should now mark it as requiring an incremental publish. 
	 * 
	 */
	@Override
	public void fileChanged(FileWatcherEvent event) {
		// DEPLOY_ASSEMBLY
		Path affected = event == null ? null : event.getPath();
		if (event == null || affected == null) {
			return;
		}
		synchronized (this) {
			//System.out.println("File changed: " + affected.toString());
			List<DeployableState> ds = new ArrayList<>(states.values());
			boolean changed = false;
			for( DeployableState d : ds ) {
				DeploymentAssemblyFile assemblyObj = assembly.get(getKey(d.getReference()));
				if( assemblyObj == null ) {
					changed |= fileChangedNoAssembly(event, affected, d);
				} else {
					changed |= fileChangedWithAssembly(event, affected, d);
				}
			}
			updateServerPublishStateFromDeployments(true);
			if (changed) {
				fireState();
			}
			launchOrUpdateAutopublishThread();
		}
		scheduleWorkspaceRefresh(event, affected);
	}

	private boolean fileChangedNoAssembly(FileWatcherEvent event, Path affected, DeployableState d) {
		boolean changed = false;
		Path deploymentPath = new File(d.getReference().getPath()).toPath();
		changed |= fileChangedSinglePath(event,  affected, d, deploymentPath);
		return changed;
	}
	

	private boolean fileChangedWithAssembly(FileWatcherEvent event, Path affected, DeployableState d) {
		boolean changed = false;
		List<Path> sourcePaths = getAssemblySourceFolders(d.getReference());
		for( int i = 0; i < sourcePaths.size(); i++ ) {
			Path deploymentPath = sourcePaths.get(i);
			changed |= fileChangedSinglePath(event, affected, d, deploymentPath);
		}
		return changed;		
	}
	
	private boolean fileChangedSinglePath(FileWatcherEvent event, Path affected, 
			DeployableState d, Path deploymentPath) {
		boolean changed = false;
		if( affected.startsWith(deploymentPath)) {
			int currentPubState = d.getPublishState();
			if (currentPubState == ServerManagementAPIConstants.PUBLISH_STATE_NONE
					|| currentPubState == ServerManagementAPIConstants.PUBLISH_STATE_INCREMENTAL) {
				int newState = getRequiredPublishStateOnFileChange(event);
				if (newState > currentPubState) {
					d.setPublishState(newState);
					changed = true;
				}
			}
			if (currentPubState == ServerManagementAPIConstants.PUBLISH_STATE_NONE
					|| currentPubState == ServerManagementAPIConstants.PUBLISH_STATE_INCREMENTAL
					|| currentPubState == ServerManagementAPIConstants.PUBLISH_STATE_FULL) {
				registerSingleDelta(event, d.getReference());
			}
		}
		return changed;
	}
	
	protected int getRequiredPublishStateOnFileChange(FileWatcherEvent event) {
		if( fullPublishRequired != null && 
				fullPublishRequired.requiresFullPublish(event)) {
			return ServerManagementAPIConstants.PUBLISH_STATE_FULL;
		}
		return ServerManagementAPIConstants.PUBLISH_STATE_INCREMENTAL;
	}
	
	private void registerSingleDelta(FileWatcherEvent event, DeployableReference reference) {
		String key = getKey(reference);
		DeployableDelta dd = getDeltas().computeIfAbsent(key, k ->  new DeployableDelta(new DeployableReference(reference)));
		DeploymentAssemblyFile assemblyMap = assembly.get(getKey(reference));
		if( assemblyMap == null ) {
			dd.registerChange(event);
		} else {
			dd.registerChange(event, assemblyMap);
		}
	}

	private void fireState() {
		if( delegate != null ) {
			delegate.fireServerStateChanged();
		}
	}

	public synchronized void updateServerPublishStateFromDeployments() {
		updateServerPublishStateFromDeployments(false);
	}
	
	public synchronized void updateServerPublishStateFromDeployments(boolean fireEvent) {
		List<DeployableState> vals = new ArrayList<>(states.values());
		int newState = ServerManagementAPIConstants.PUBLISH_STATE_NONE;

		if (deployableExists(ServerManagementAPIConstants.PUBLISH_STATE_ADD, vals)
				|| deployableExists(ServerManagementAPIConstants.PUBLISH_STATE_REMOVE, vals)
				|| deployableExists(ServerManagementAPIConstants.PUBLISH_STATE_FULL, vals)) {
			newState = ServerManagementAPIConstants.PUBLISH_STATE_FULL;
		} else if (deployableExists(ServerManagementAPIConstants.PUBLISH_STATE_UNKNOWN, vals)) {
			newState = ServerManagementAPIConstants.PUBLISH_STATE_UNKNOWN;
		} else if (deployableExists(ServerManagementAPIConstants.PUBLISH_STATE_INCREMENTAL, vals)) {
			newState = ServerManagementAPIConstants.PUBLISH_STATE_INCREMENTAL;
		}
		setServerPublishState(newState, fireEvent);
	}

	private boolean deployableExists(int publishState, List<DeployableState> deployableStates) {
		return deployableStates.stream().anyMatch(deployState -> deployState.getPublishState() == publishState);
	}

	@Override
	public synchronized int getServerPublishState() {
		return this.publishState;
	}

	@Override
	public synchronized void setServerPublishState(int state, boolean fire) {
		if (state != this.publishState) {
			this.publishState = state;
			if (fire) {
				fireState();
			}
		}
	}

	@Override
	public synchronized DeployableReference fillOptionsFromCache(DeployableReference reference) {
		if (reference == null) {
			return null;
		}
		reference.setOptions(deploymentOptions.get(getKey(reference)));
		return reference;
	}

	@Override
	public synchronized IDeployableDelta getDeployableResourceDelta(DeployableReference reference) {
		return cloneDelta(deltas.get(getKey(reference)));
	}
	
	private IDeployableDelta cloneDelta(DeployableDelta delta) {
		if( delta == null )
			return null;
		DeployableReference ref = cloneReference(delta.getReference());
		return new DeployableDelta(ref, delta.getResourceDeltaMap());
	}
	private DeployableReference cloneReference(DeployableReference ref) {
		return ref == null ? null : new DeployableReference(ref);
	}

	protected boolean isAutoPublisherEnabled() {
		if (!isAutoPublisherSupported()) {
			return false;
		}
		return delegate.getServer().getAttribute(
				DefaultServerAttributes.AUTOPUBLISH_ENABLEMENT, 
				DefaultServerAttributes.AUTOPUBLISH_ENABLEMENT_DEFAULT);
	}

	private boolean isAutoPublisherSupported() {
		if (delegate == null || delegate.getServer() == null) {
			return true;
		}
		String typeId = delegate.getServer().getTypeId();
		return typeId == null || !typeId.startsWith("com.ibm.ws.ast");
	}
	
	protected int getInactivityTimeout() {
		return delegate.getServer().getAttribute(
				DefaultServerAttributes.AUTOPUBLISH_INACTIVITY_LIMIT, 
				DefaultServerAttributes.AUTOPUBLISH_INACTIVITY_LIMIT_DEFAULT);
	}

	protected void launchOrUpdateAutopublishThread() {
		if (isAutoPublisherEnabled()) {
			launchOrUpdateAutopublishThreadImpl();
		}
	}
	protected void launchOrUpdateAutopublishThreadImpl() {
		synchronized (this) {
			if (this.autoPublish != null) {
				if (this.autoPublish.isDone() || this.autoPublish.getPublishBegan()) {
					// we need a new thread
					this.autoPublish = createNewAutoPublishThread( getInactivityTimeout());
					this.autoPublish.start();
				} else {
					this.autoPublish.updateInactivityCounter();
				}
			} else {
				this.autoPublish = createNewAutoPublishThread( getInactivityTimeout());
				this.autoPublish.start();
			}
		}
	}
	
	protected WSTAutoPublishThread createNewAutoPublishThread(int timeout) {
		return new WSTAutoPublishThread(delegate.getServer(), timeout, this::hasPendingChanges, this::isAutoPublisherEnabled);
	}

	public synchronized void markPublished() {
		for (DeployableDelta delta : deltas.values()) {
			if (delta != null) {
				delta.clear();
			}
		}
		synchronizeStatesFromControl();
		updateServerPublishStateFromDeployments();
		fireState();
	}

	private boolean hasPendingChanges() {
		for (DeployableDelta delta : deltas.values()) {
			if (delta != null && !delta.getResourceDeltaMap().isEmpty()) {
				return true;
			}
		}
		return false;
	}

	private void handleAutoPublishConfigChanged() {
		if (!isAutoPublisherEnabled()) {
			cancelAutoPublishThread();
			return;
		}
		if (hasPendingChanges() || getServerPublishState() != ServerManagementAPIConstants.PUBLISH_STATE_NONE) {
			launchOrUpdateAutopublishThread();
		}
	}

	private void cancelAutoPublishThread() {
		if (autoPublish != null) {
			autoPublish.cancel();
			autoPublish = null;
		}
	}

	private void registerAutoPublishConfigListener() {
		if (autoPublishListenerRegistered || delegate == null || delegate.getServer() == null) {
			return;
		}
		IServerModel model = delegate.getServer().getServerModel();
		if (model != null) {
			model.addServerModelListener(autoPublishListener);
			autoPublishListenerRegistered = true;
		}
	}

	private void registerPublishStateListener() {
		IWstServerControl control = resolveWstServerControl();
		if (publishStateListenerRegistered || control == null) {
			return;
		}
		control.addServerListener(publishStateListener);
		publishStateListenerRegistered = true;
	}

	private IStatus registerDeployable(DeployableReference reference) {
		String key = getKey(reference);
		if (reference.getOptions() != null || !deploymentOptions.containsKey(key)) {
			deploymentOptions.put(key, reference.getOptions());
		}
		return registerFileWatcher(reference);
	}

	private static class WorkspaceResolution {
		private final IProject project;
		private final IResource resource;

		private WorkspaceResolution(IProject project, IResource resource) {
			this.project = project;
			this.resource = resource;
		}

		private IProject getProject() {
			return project;
		}

		private IResource getResource() {
			return resource;
		}
	}

	private static class WorkspaceRefreshTarget {
		private final Path path;
		private final IProject project;
		private final IResource resource;
		private final int depth;

		private WorkspaceRefreshTarget(Path path, IProject project, IResource resource, int depth) {
			this.path = path;
			this.project = project;
			this.resource = resource;
			this.depth = depth;
		}

		private Path getPath() {
			return path;
		}

		private IProject getProject() {
			return project;
		}

		private IResource getResource() {
			return resource;
		}

		private int getDepth() {
			return depth;
		}
	}

	private static class PendingWorkspaceRefresh {
		private final long sequence;
		private final WorkspaceRefreshTarget target;

		private PendingWorkspaceRefresh(long sequence, WorkspaceRefreshTarget target) {
			this.sequence = sequence;
			this.target = target;
		}

		private long getSequence() {
			return sequence;
		}

		private WorkspaceRefreshTarget getTarget() {
			return target;
		}
	}

}
