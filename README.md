# The Moonlight Controller

Moonlight Controller, or the OpenBox controller, is the control server for several OBSIs (OpenBox Data-Plane Instances).

The framework depends on Oracle Java 8. The installation script is no longer able to install Java as the Oracle License has changed in an incompatible manner. Hence, one shall download and install Oracle Java 8 (1.8) from their website in order for the framework to operate.

After installing Java, Maven should be installed. Maven installs OpenJDK 11 (for the time of writing) by default and overrides other versions of Java on the machine, thus, Oracle Java 8 must be configured as the default Java afterwards.

Other than that, the install script creates `start_moonlight` script which should be used to start the controller.

In the project directory there exists a `target` directory. All relevant resources, like, firewall rules, DPI patterns, etc should be stored there. Inside the target directory, an `app` directory should contain the JAR files of the applications which should be loaded into the controller. The topology file should be edited with the uuids of the relevant existing OBSIs to communicate with.



