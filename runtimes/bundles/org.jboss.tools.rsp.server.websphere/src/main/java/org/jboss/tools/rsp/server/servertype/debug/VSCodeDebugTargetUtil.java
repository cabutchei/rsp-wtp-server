package org.jboss.tools.rsp.server.servertype.debug;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.wst.server.core.IRuntime;

import com.ibm.ws.ast.st.common.core.internal.IDebugTargetUtil;



public class VSCodeDebugTargetUtil implements IDebugTargetUtil {
    
    @Override
    public boolean isDebugConnected(String host, int port) {
        return true;
    }

    @Override
    public IDebugTarget createDebugTarget(ILaunch processLaunch, String serverAdminHostName, String debugPort, String label, IRuntime runtime) {
        return new IDebugTarget() {
            @Override
            public IProcess getProcess() { return null; }
            @Override
            public IDebugTarget getDebugTarget() { return this; }
            @Override
            public boolean isTerminated() { return false; }
            @Override
            public boolean isSuspended() { return false; }
            @Override
            public boolean canTerminate() { return false; }
            @Override
            public boolean canResume() { return false; }
            @Override
            public boolean canSuspend() { return false; }
            @Override
            public void terminate() throws DebugException {}
            @Override
            public void resume() throws DebugException {}
            @Override
            public void suspend() throws DebugException {}
            @Override
            public void breakpointAdded(IBreakpoint breakpoint) {}
            @Override
            public void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta) {}
            @Override
            public void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta) {}
            @Override
            public boolean supportsStorageRetrieval() { return false; }
            @Override
            public IMemoryBlock getMemoryBlock(long startAddress, long length) throws DebugException { return null; }
            @Override
            public Object getAdapter(Class adapter) { return null; }
            @Override
            public IThread[] getThreads() throws DebugException { return new IThread[0]; }
            @Override
            public boolean hasThreads() throws DebugException { return false; }
            @Override
            public String getName() throws DebugException { return ""; }
            @Override
            public boolean supportsBreakpoint(IBreakpoint var1) { return false; }
            @Override
            public boolean canDisconnect() { return false; }
            @Override
            public void disconnect() throws DebugException {}
            @Override
            public boolean isDisconnected() { return false; }
            @Override
            public String getModelIdentifier() { return ""; }
            @Override
            public ILaunch getLaunch() { return processLaunch; }
        };
            
    }
}