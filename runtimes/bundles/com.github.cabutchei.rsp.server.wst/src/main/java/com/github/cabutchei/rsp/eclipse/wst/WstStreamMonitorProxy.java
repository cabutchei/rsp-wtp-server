package com.github.cabutchei.rsp.eclipse.wst;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.github.cabutchei.rsp.eclipse.debug.core.IStreamListener;
import com.github.cabutchei.rsp.eclipse.debug.core.model.IFlushableStreamMonitor;
import com.github.cabutchei.rsp.eclipse.core.runtime.SafeRunner;
import com.github.cabutchei.rsp.eclipse.core.runtime.ISafeRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class WstStreamMonitorProxy implements IFlushableStreamMonitor {
	private static final Logger LOG = LoggerFactory.getLogger(WstStreamMonitorProxy.class);
	private final org.eclipse.debug.core.model.IStreamMonitor wstMonitor;
	private final Map<IStreamListener, org.eclipse.debug.core.IStreamListener> listenerMap = new HashMap<>();

	public WstStreamMonitorProxy(org.eclipse.debug.core.model.IStreamMonitor wstMonitor) {
		this.wstMonitor = Objects.requireNonNull(wstMonitor, "wstMonitor");
	}
	

	@Override
	public void addListener(IStreamListener listener) {
		if (listener == null) {
			return;
		}
		org.eclipse.debug.core.IStreamListener wrapped = new org.eclipse.debug.core.IStreamListener() {
			@Override
			public void streamAppended(String text, org.eclipse.debug.core.model.IStreamMonitor monitor) {
				listener.streamAppended(text, WstStreamMonitorProxy.this);
			}
		};
		listenerMap.put(listener, wrapped);
		wstMonitor.addListener(wrapped);
		this.flushAndDisableBuffer(listener);
		listener.streamAppended(getContents(), WstStreamMonitorProxy.this);
	}

	private void flushAndDisableBuffer(IStreamListener listener) {
		byte[] data = null;
		String contents = getContents();
		this.flushContents();
		this.setBuffered(false);
		listener.streamAppended(contents, this);
}

	@Override
	public String getContents() {
		return wstMonitor.getContents();
	}

	@Override
	public void removeListener(IStreamListener listener) {
		if (listener == null) {
			return;
		}
		org.eclipse.debug.core.IStreamListener wrapped = listenerMap.remove(listener);
		if (wrapped != null) {
			wstMonitor.removeListener(wrapped);
		}
	}

	/**
	 * Notifies the listeners that text has
	 * been appended to the stream.
	 * @param text the text that was appended to the stream
	 */
	private void fireStreamAppended(String text) {
		getNotifier().notifyAppend(text);
	}

	private ContentNotifier getNotifier() {
		return new ContentNotifier();
	}

	public void setBuffered(boolean buffer) {
		if (this.wstMonitor instanceof IFlushableStreamMonitor) {
			IFlushableStreamMonitor m = (IFlushableStreamMonitor) this.wstMonitor;
			m.setBuffered(buffer);
		}
   	}

   public void flushContents() {
		if (this.wstMonitor instanceof IFlushableStreamMonitor) {
			IFlushableStreamMonitor m = (IFlushableStreamMonitor) this.wstMonitor;
			m.flushContents();
		}
   }

   public boolean isBuffered() {
		if (this.wstMonitor instanceof IFlushableStreamMonitor) {
			IFlushableStreamMonitor m = (IFlushableStreamMonitor) this.wstMonitor;
			return m.isBuffered();
		}
		return false;
   }


	class ContentNotifier implements ISafeRunnable {

		private IStreamListener fListener;
		private String fText;

		/**
		 * @see com.github.cabutchei.rsp.eclipse.core.runtime.ISafeRunnable#handleException(java.lang.Throwable)
		 */
		@Override
		public void handleException(Throwable exception) {
			log(exception);
		}

		/**
		 * @see com.github.cabutchei.rsp.eclipse.core.runtime.ISafeRunnable#run()
		 */
		@Override
		public void run() throws Exception {
			fListener.streamAppended(fText, WstStreamMonitorProxy.this);
		}

		public void notifyAppend(String text) {
			if (text == null) {
				return;
			}
			fText = text;
			for (IStreamListener iStreamListener : listenerMap.keySet()) {
				fListener = iStreamListener;
				SafeRunner.run(this);
			}
			fListener = null;
			fText = null;
		}
	}

	private void log(Throwable t) {
		String msg = (t == null || t.getMessage() == null ? "Unknown Error" : t.getMessage());
		LOG.error(msg, t);
	}
}
