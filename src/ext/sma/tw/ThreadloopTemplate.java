package ext.sma.tw;

import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;

import org.json.JSONObject;

import com.thingworx.logging.LogUtilities;
import com.thingworx.metadata.annotations.ThingworxBaseTemplateDefinition;
import com.thingworx.metadata.annotations.ThingworxConfigurationTableDefinition;
import com.thingworx.metadata.annotations.ThingworxConfigurationTableDefinitions;
import com.thingworx.metadata.annotations.ThingworxDataShapeDefinition;
import com.thingworx.metadata.annotations.ThingworxFieldDefinition;
import com.thingworx.metadata.annotations.ThingworxServiceDefinition;
import com.thingworx.metadata.annotations.ThingworxServiceParameter;
import com.thingworx.metadata.annotations.ThingworxServiceResult;
import com.thingworx.security.context.SecurityContext;
import com.thingworx.system.ContextType;
import com.thingworx.system.configuration.PlatformSettings;
import com.thingworx.things.Thing;
import com.thingworx.types.collections.ValueCollection;
import com.thingworx.types.primitives.JSONPrimitive;
import com.thingworx.types.primitives.LongPrimitive;
import com.thingworx.types.primitives.StringPrimitive;
import com.thingworx.webservices.context.ThreadLocalContext;

@ThingworxConfigurationTableDefinitions(tables = {
		@ThingworxConfigurationTableDefinition(name = ThreadloopTemplate.THREADLOOP_SETTINGS, description = "Settings for the Threadloop", isMultiRow = false, dataShape = @ThingworxDataShapeDefinition(fields = {
				@ThingworxFieldDefinition(name = ThreadloopTemplate.THREADLOOP_PAUSE, description = "Pause between Runs in miliseconds.", baseType = "INTEGER", aspects = {
						"defaultValue:30000", "friendlyName:Threadloop Pause" }, ordinal = 1) })) })

@ThingworxBaseTemplateDefinition(name = "GenericThing")
public class ThreadloopTemplate extends Thing {

	private static final long serialVersionUID = 768504155433223986L;

	// Logger instance
	protected static final Logger _logger = LogUtilities.getInstance().getApplicationLogger(ThreadloopTemplate.class);

	static final String THREADLOOP_SETTINGS = "threadloopSettings";
	static final String THREADLOOP_PAUSE = "threadloopPause";

	private final AtomicReference<Thread> _workerRef = new AtomicReference<>();
	private String _plaformId = null;
	private int _pause = 30000;

	public ThreadloopTemplate() {
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void startThing(ContextType contextType) throws Exception {
		super.startThing(contextType);

		_plaformId = PlatformSettings.getInstance().getPlatformId();
		_pause = this.getPauseConfig();
		this.startThreadloop();
	}

	@Override
	public void stopThing(ContextType contextType) {
		this.stopThreadloop();
	}

	private int getPauseConfig() {
		return ((Integer) getConfigurationSetting(THREADLOOP_SETTINGS, THREADLOOP_PAUSE)).intValue();
	}

	private void startThreadloop() {

		final Thread worker = new Thread("threadloop-" + this.getName()) {

			@Override
			public void run() {

				long last_wakeup_time = System.currentTimeMillis();
				JSONObject data = new JSONObject();

				while (true) {
					// Sleep until the next scheduled check.
					try {
						Thread.sleep(_pause);
						final long now = System.currentTimeMillis();
						final long effective_pause = now - last_wakeup_time;

						try {
							ThreadLocalContext.setSecurityContext(SecurityContext.createSystemUserContext());

							ValueCollection params = new ValueCollection();
							params.put("platformId", new StringPrimitive(_plaformId));
							params.put("effectivePause", new LongPrimitive((Number) effective_pause));
							params.put("data", new JSONPrimitive(data));
							data = (JSONObject) processServiceRequest("Run", params).getReturnValue();
						} finally {
							ThreadLocalContext.clearSecurityContext();
							last_wakeup_time = now;
						}

					} catch (InterruptedException e) {
						if (_workerRef.compareAndSet(this, null))
							_logger.info(getName() + " thread has been interrupted.");
						else
							_logger.info(getName() + " thread has been stopped.");
						return;
					} catch (Exception e) {
						_logger.error("Error while executing Run callback service on " + getName(), e);
					}
				}
			}
		};

		if (!_workerRef.compareAndSet(null, worker)) {
			_logger.warn(getName() + " thread already started.");
			return;
		}

		worker.start();
		_logger.info(getName() + " thread started.");
	}

	private void stopThreadloop() {

		final Thread worker = _workerRef.getAndSet(null);

		if (worker != null && worker.isAlive() && !worker.isInterrupted()) {
			_logger.warn("Interrupting " + getName() + " Threadloop thread.");
			worker.interrupt();
		}
	}

	@ThingworxServiceDefinition(name = "Run", description = "Threadloop callback to override", category = "", isAllowOverride = true, aspects = {
			"isAsync:false" })
	@ThingworxServiceResult(name = "result", description = "", baseType = "JSON", aspects = {})
	public JSONObject Run(
			@ThingworxServiceParameter(name = "platformId", description = "Platform node id", baseType = "STRING") String platformId,
			@ThingworxServiceParameter(name = "effectivePause", description = "Effective pause in milliseconds", baseType = "LONG", aspects = {
					"isRequired:true", "units:millisecond" }) Long effectivePause,
			@ThingworxServiceParameter(name = "data", description = "", baseType = "JSON") JSONObject data) {
		return data;
	}
}
