About Tycho.setup
=================

Tycho.setup is an Eclipse Installer (Oomph) setup file that fulfills the following tasks:

  * Install Eclipse
  * Install all required m2e connectors
  * Clone the Tycho git repository
  * Clone the Tycho Extra git repository
  * Create working sets for Tycho and Tycho Extras
  * Import the Tycho and Tycho Extra projects as maven projects
  * Set the target platform to tycho-bundles/tycho-bundles-target/tycho-bundles-target.target
  
Usage for contributors:
-----------------------

The Tycho.setup file is part of the Eclipse Installer Project Catalog, so the Tycho.setup file doesn't have to be downloaded manually.

  * Download the [Eclipse Installer](https://wiki.eclipse.org/Eclipse_Installer)
  * Run the Eclipse Installer
  * Switch to the Advanced Mode (Menu->"Advanced Mode...")
  * Select "Eclipse IDE for Eclipse Committers"
  * Select "Latest Release (year-month)" as "Product Version".
  * Choose one of the installed JREs (must be 11+) as "Java 11+ VM"
  * Click "Next"
  * Find and select "Tycho" under the "Eclipse Projects" catalog. Make sure that Tycho is shown in the table on the bottom.
  * Click "Next" to get the Variables page
  * You can edit the values used by the setup process (e.g. the "Installation Folder"). If you want to choose the Install Folder and "Root install folder", you have to select "Show all variables" at the bottom of the page. Then you are also able to specify the location where Eclipse will be installed. You do not have to select the Target Platform here, this will be set later automatically.
  * Unless you have write access to the GitHub repository, make sure you select "HTTPS read-only" in the dropdown "Tycho Github repository".
  * Click "Next"
  * Pressing "Finished" on the "Confirmation" page will start the installation process. 
  * The installer will download the selected Eclipse version, will start Eclipse and will perform all the additional steps (cloning the git repos etc.). The progress bar in the status bar shows the progress of the overall installation.
  * Once the "Executing startup tasks" job is finished you should end up having an Eclipse with all the Tycho and Tycho Extras projects imported in the workspace.
  * Some Projects might sill have errors. Select the projects with errors and choose "Maven->Update Project.." from the context menu. De-select "Clean projects" in the shown dialog and press "OK" to update the projects. After that no more error should be there.  

Make changes to the Tycho.setup file
------------------------------------

   * In order to change the Tycho.setup file, the easiest way to do that is to follow the instructions above by using the Eclipse Installer to install a Tycho development environment. 
   * When the setup process is finished, choose "Navigate"->"Open Setup"->"Open <User> - Tycho - master" in the Eclipse menu. 
   * The Setup Editor will be opened with the Tycho.setup file. 
   * See [Eclipse Oomph Authoring](https://wiki.eclipse.org/Eclipse_Oomph_Authoring) for details.
