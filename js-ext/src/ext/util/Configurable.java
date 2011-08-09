package ext.util;

public interface Configurable {

	/**
	 * Configure the object. This method should drag all required parameters to
	 * instantiate the module out of the given configuration and do all required
	 * initialization.
	 * 
	 * @param config
	 *            Properties that contain arbitrary configuration parameters
	 *            relevant to the module
	 * @throws Exception
	 */
	public void configure(ExtendedProperties config) throws Exception;

}
