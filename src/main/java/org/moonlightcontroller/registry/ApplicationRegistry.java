package org.moonlightcontroller.registry;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.Random;
import java.util.ServiceLoader;

import org.moonlightcontroller.bal.BoxApplication;
import org.moonlightcontroller.mtd.IApplicationType;

/**
 * Application registry is responsible for loading applications into the OBC
 * It is an Implementation for the IApplicationRegistry interface.
 */
public class ApplicationRegistry implements IApplicationRegistry {

	private Map<String, BoxApplication> apps, apps_by_name;
	private Map<IApplicationType, List<BoxApplication>> apps_by_type;
	
	public ApplicationRegistry(){
		this.apps_by_name= new HashMap<>();
		this.apps_by_type = new HashMap<>();
		this.apps = apps_by_name;
	}
	@Override
	public void addApplication(BoxApplication app) {
		this.apps_by_name.put(app.getName(), app);
		this.apps_by_type.putIfAbsent(app.getType(), new ArrayList<BoxApplication>());
		this.apps_by_type.get(app.getType()).add(app);
	}
	@Override
	public boolean loadFromPath(String path) throws IOException{
		File loc = new File(path);

		if (!loc.exists()) {
			System.out.println("Given path is not found:" + path);
			return false;
		}
		if (!loc.isDirectory()){
			System.out.println("Path is not a directory:" + path);
			return false;
		}
		
		File[] flist = loc.listFiles(new FileFilter() {
		public boolean accept(File file) {
			return file.getPath().toLowerCase().endsWith(".jar");
			}
		});
		
		
		URL[] urls = new URL[flist.length];
		for (int i = 0; i < flist.length; i++){
			urls[i] = flist[i].toURI().toURL();
		}
		URLClassLoader ucl = new URLClassLoader(urls);

		//System.out.println(BoxApplication.class);
		ServiceLoader<BoxApplication> sl = ServiceLoader.load(BoxApplication.class, ucl);
		Iterator<BoxApplication> apit = sl.iterator();
		while (apit.hasNext()){
			BoxApplication app = apit.next();
			System.out.printf("Registry: Found application: %s of type %d %n", app.getName(), app.getType());
			this.addApplication(app);
		}
		return true;
	}
	
	@Override
	public List<BoxApplication> getApplications() {
		List<BoxApplication> list = new ArrayList<>(this.apps.values());
		return list;
	}
	@Override
	public BoxApplication getApplicationByName(String name) {
		return this.apps.get(name);
	}

	@Override
	public List<BoxApplication> getApplicationVariants(IApplicationType type) {
		return this.apps_by_type.get(type);
	}

	@Override
	public Set<IApplicationType> getApplicationTypes() {
		return this.apps_by_type.keySet();
	}
}
