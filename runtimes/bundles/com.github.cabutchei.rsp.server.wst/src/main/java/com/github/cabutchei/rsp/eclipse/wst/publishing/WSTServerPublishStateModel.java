package com.github.cabutchei.rsp.eclipse.wst.publishing;



import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

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
	
	private final Map<String, Map<String,Object>> deploymentOptions;
	private final Map<String, DeployableDelta> deltas = new HashMap<>();
	private final Map<String, DeploymentAssemblyFile> assembly = new HashMap<>();
	
	private AbstractServerDelegate delegate;
    private IWstServerControl wstServerControl;
    private Supplier<IWstServerControl> controlSupplier;
	private IFileWatcherService fileWatcher;
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
			if ((event.getKind() & ServerEvent.PUBLISH_STATE_CHANGE) == 0) {
				return;
			}
			if (!isAutoPublisherEnabled()) {
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
        this.controlSupplier = controlSupplier;
		this.wstServerControl = this.controlSupplier.get();
		this.fileWatcher = fileWatcher;
		this.fullPublishRequired = fullPublishRequired;
		this.deploymentOptions = new LinkedHashMap<>();
	}

	public WSTServerPublishStateModel(AbstractServerDelegate delegate, IWstServerControl wstServerControl,
			IFileWatcherService fileWatcher, IFullPublishRequiredCallback fullPublishRequired) {
		this.delegate = delegate;
		this.wstServerControl = wstServerControl;
		this.fileWatcher = fileWatcher;
		this.fullPublishRequired = fullPublishRequired;
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

		List<DeployableState> existing = wstServerControl == null
				? Collections.emptyList()
				: wstServerControl.getDeployableStates();
		if (existing != null && !existing.isEmpty()) {
			for (DeployableState state : existing) {
				DeployableReference ref = state.getReference();
				if (ref != null) {
					registerDeployable(ref);
				}
			}
		} else if (references != null && !references.isEmpty()) {
			for (DeployableReference reference : references) {
				registerDeployable(reference);
			}
		}
		updateServerPublishStateFromDeployments();
		fireState();
		registerAutoPublishConfigListener();
		// registerPublishStateListener();
	}

	private DeployableState cloneDeployableState(DeployableReference reference, DeployableState state) {
		return createDeployableState(reference, state.getPublishState(), state.getState());
	}
	
	private DeployableState createDeployableState(DeployableReference reference, int publishState, int state) {
		DeployableState deployableState = new DeployableState();
		deployableState.setPublishState(publishState);
		deployableState.setState(state);
		deployableState.setReference(new DeployableReference(reference.getLabel(), reference.getPath()));
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
		IStatus status = this.wstServerControl.addDeployable(reference);
		if (!status.isOK()) {
			return status;
		}
		return registerDeployable(reference);
	}

	@Override
	public synchronized boolean contains(DeployableReference reference) {
		if (reference == null) {
			return false;
		}
		String key = getKey(reference);
		for (DeployableState state : getWstDeployableStates()) {
			DeployableReference ref = state.getReference();
			if (ref != null && key.equals(getKey(ref))) {
				return true;
			}
		}
		return false;
	}

	@Override
	public synchronized IStatus removeDeployable(DeployableReference reference) {
		if (!contains(reference)) {
			return new Status(IStatus.ERROR, ServerCoreActivator.BUNDLE_ID, IStatus.ERROR, 
					NLS.bind("Could not remove deploybale with path {0}: it doesn't exist", getKey(reference)),
							null);
		}
		IStatus status = this.wstServerControl.removeDeployable(reference);
		if (!status.isOK()) {
			return status;
		}
		if (fileWatcher != null) {
			List<Path> sourcePaths = getDeploySourceFolders(reference);
			for( int i = 0; i < sourcePaths.size(); i++ ) {
				Path sourcePathToWatch = sourcePaths.get(i);
				fileWatcher.removeFileWatcherListener(sourcePathToWatch, this);
			}
		}
		deployableRemoved(reference);
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

	private List<DeployableState> getWstDeployableStates() {
		if (wstServerControl == null) {
			return Collections.emptyList();
		}
		List<DeployableState> states = wstServerControl.getDeployableStates();
		return states == null ? Collections.emptyList() : states;
	}

	private void refreshWorkspaceForPath(Path affected) {
		if (affected == null) {
			return;
		}
		IProject project = resolveProjectForPath(affected);
		if (project == null || !project.exists()) {
			return;
		}
		try {
			project.refreshLocal(IResource.DEPTH_INFINITE, null);
		} catch (org.eclipse.core.runtime.CoreException e) {
			LOG.warn("Failed to refresh workspace for {}", affected, e);
		}
	}

	private IProject resolveProjectForPath(Path path) {
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
			return resource.getProject();
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
		return bestMatch;
	}
	
	@Override
	public synchronized void deployableRemoved(DeployableReference reference) {
		String k = getKey(reference);
		deploymentOptions.remove(k);
		deltas.remove(k);
		assembly.remove(k);
	}

	@Override
	public synchronized List<DeployableState> getDeployableStates() {
		List<DeployableState> states = getWstDeployableStates();
		List<DeployableState> ret = new ArrayList<>(states.size());
		for (DeployableState state : states) {
			DeployableReference ref = state.getReference();
			if (ref != null) {
				fillOptionsFromCache(ref);
			}
			ret.add(state);
		}
		return ret;
	}

	@Override
	public synchronized List<DeployableState> getDeployableStatesWithOptions() {
		List<DeployableState> states = getWstDeployableStates();
		List<DeployableState> ret = new ArrayList<>(states.size());
		for (DeployableState ds : states) {
			DeployableReference ref = ds.getReference();
			if (ref != null) {
				fillOptionsFromCache(ref);
			}
			ret.add(ds);
		}
		return ret;
	}

	@Override
	public synchronized DeployableState getDeployableState(DeployableReference reference) {
		if (reference == null || wstServerControl == null) {
			return null;
		}
		DeployableState state = wstServerControl.getDeployableState(reference);
		if (state != null && state.getReference() != null) {
			fillOptionsFromCache(state.getReference());
		}
		return state;
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
		if (reference == null) {
			return;
		}
		String key = getKey(reference);
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
		// WST manages run state; nothing to do here.
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
	public synchronized void fileChanged(FileWatcherEvent event) {
		// DEPLOY_ASSEMBLY
		Path affected = event.getPath();
		//System.out.println("File changed: " + affected.toString());
		List<DeployableState> ds = new ArrayList<>(getWstDeployableStates());
		boolean changed = false;
		for( DeployableState d : ds ) {
			DeploymentAssemblyFile assemblyObj = assembly.get(getKey(d.getReference()));
			if( assemblyObj == null ) {
				changed = fileChangedNoAssembly(event, affected, d);
			} else {
				changed = fileChangedWithAssembly(event, affected, d);
			}
		}
		refreshWorkspaceForPath(affected);
		updateServerPublishStateFromDeployments(true);
		if (changed) {
			fireState();
		}
		launchOrUpdateAutopublishThread();
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
			registerSingleDelta(event, d.getReference());
			changed = true;
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
		DeployableDelta dd = getDeltas().computeIfAbsent(key, k ->  new DeployableDelta(new DeployableReference(reference.getLabel(), reference.getPath())));
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
		if (wstServerControl == null) {
			return;
		}
		setServerPublishState(wstServerControl.getServerPublishState(), fireEvent);
	}

	@Override
	public synchronized int getServerPublishState() {
		if (wstServerControl != null) {
			this.publishState = wstServerControl.getServerPublishState();
		}
		return this.publishState;
	}

	@Override
	public synchronized void setServerPublishState(int state, boolean fire) {
		int nextState = state;
		if (wstServerControl != null) {
			nextState = wstServerControl.getServerPublishState();
		}
		if (nextState != this.publishState) {
			this.publishState = nextState;
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
		return ref == null ? null : new DeployableReference(ref.getLabel(), ref.getPath());
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
		if (hasPendingChanges() || (wstServerControl != null
				&& wstServerControl.getServerPublishState() != ServerManagementAPIConstants.PUBLISH_STATE_NONE)) {
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
		if (publishStateListenerRegistered || wstServerControl == null) {
			return;
		}
		wstServerControl.addServerListener(publishStateListener);
		publishStateListenerRegistered = true;
	}

	private IStatus registerDeployable(DeployableReference reference) {
		deploymentOptions.put(getKey(reference), reference.getOptions());
		return registerFileWatcher(reference);
	}

}
