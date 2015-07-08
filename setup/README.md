About Tycho.setup
=================

Tycho.setup is an Eclipse Installer (Oomph) setup file that does

  * Install Eclipse
  * Installs all required m2e connectors
  * Clone the Tycho git repository
  * Clone the Tycho Extra git repository
  * Creates working sets for Tycho and Tycho Extras
  * Imports the Tycho and Tycho Extra projects as maven projects
  * Sets the target platform to tycho-bundles/tycho-bundles-target/tycho-bundles-target.target
  
Usage for contributors:
-----------------------

The Tycho.setup file is not part of the Eclipse Installer Project Catalog yet, so the Tycho.setup file must be downloaded and opened by the Eclipse Installer manually. 

  * Download the [Eclipse Installer](https://wiki.eclipse.org/Eclipse_Installer)
  * Download the Tycho.setup file
  * Run the Eclipse Installer
  * Switch to the Advanced Mode (Menu->"Advanced Mode...")
  * Select "Eclipse IDE for Java Developers"
  * Select "Latest Release (Mars)" as "Product Version" and select either 32 or 64 bit version
  * Choose one of the installed JREs (must be 1.7+) as "Java 1.7+ VM"
  * Click "Next"
  * The "Eclipse.org" and "Github.com" catalogs are shown. Since the Tycho.setup file is not part of the catalog yet, 
    you have to Drag and Drop the Tycho.setup file into this view 
    (Note: It must be dropped either on the folder "Eclipse.org" or the "Github.com")
  * Now there must be a <User>/Tycho entry in the container where you dropped the Tycho.setup file. 
  * Double click (Single click is not enough!) on the <User>/Tycho entry will select the Tycho.setup to be executed. Make sure that <User>/Tycho is shown in the table on the bottom.
  * Click "Next" to get the the Variables page
  * You can edit the values used by the setup process (e.g. the "Installation Folder"). If you want to choose the Install Folder and "Root install folder", you have to select "Show all variables" at the bottom of the page. Then you are also able to specify the location where Eclipse will be installed. You do not have to select the Target Platform here, this will be set later automatically.
  * Click "Next"
  * Pressing "Finished" on the "Confirmation" page will start the installation process. 
  * The installer will download the selected Eclipse version, will start Eclipse and will perform all the additional steps (cloning the git repos, etc...). The progress bar in the status bar shows the progress of the overall installation.
  * Once the "Executing startup tasks" job is finished you should end up having an Eclipse with all the Tycho and Tycho Extras projects imported in the workspace.
  * Some Projects might sill have errors. Select them and choose "Maven->Update Project.." from the context menu.
  
Make changes to the Tycho.setup file
------------------------------------

   * In order to change the Tycho.setup file, the easiest way to to that is to follow the instructions above by using the Eclipse Installer to install a Tycho development environment. 
   * When the setup process is finished, choose "Navigate"->"Open Setup"->"Open <User> - Tycho - master" in the Eclipse menu. 
   * The Setup Editor will be opened with the Tycho.setup file. 
   * See [Eclipse Oomph Authoring](https://wiki.eclipse.org/Eclipse_Oomph_Authoring) for details.
   