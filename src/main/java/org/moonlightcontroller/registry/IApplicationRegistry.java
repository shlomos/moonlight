package org.moonlightcontroller.registry;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.moonlightcontroller.bal.BoxApplication;
import org.moonlightcontroller.mtd.IApplicationType;

/**
 * The registry interface is responsible for registering applications in the OBC
 */
public interface IApplicationRegistry {
	/**
	 * Add an application to the OBC
	 * @param app
	 */
	void addApplication(BoxApplication app);

	/**
	 * Loads all applications from a given path
	 * @param path
	 * @return
	 * @throws IOException
	 */
	boolean loadFromPath(String path) throws IOException;
	
	/**
	 * Gets all loaded applications
	 * @return
	 */
	List<BoxApplication> getApplications();
	
	/**
	 * Gets an application instance by it's name
	 * @param name
	 * @return
	 */
	BoxApplication getApplicationByName(String name);

	/**
	 * Gets an application variant by type.
	 * @param type
	 * @return List of applications for the given type
	 */
	List<BoxApplication> getApplicationVariants(IApplicationType type);

	/**
	 * Gets all application types registered.
	 * @return All registered application types
	 */
	public Set<IApplicationType> getApplicationTypes();
}
