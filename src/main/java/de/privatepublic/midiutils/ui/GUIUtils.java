package de.privatepublic.midiutils.ui;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import org.apache.commons.io.FilenameUtils;

import de.privatepublic.midiutils.Prefs;

public class GUIUtils {

	
	public static File loadDialog(String dialogTitle, FileFilter fileFilter, String prefsKeyRecentFile) {
		String recentPath = Prefs.get(prefsKeyRecentFile, null);
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogType(JFileChooser.OPEN_DIALOG);
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        chooser.setMultiSelectionEnabled(false);
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.addChoosableFileFilter(fileFilter);
        chooser.setDialogTitle(dialogTitle);
        if (recentPath!=null) {
        	chooser.setSelectedFile(new File(recentPath));
        	chooser.setCurrentDirectory(new File(recentPath).getParentFile());
        }
        int retvalue = chooser.showDialog(null, "Load");
        if (retvalue==JFileChooser.APPROVE_OPTION) {
        	return chooser.getSelectedFile();
        }
        return null;
	}
	
	
	@SuppressWarnings("serial")
	public static File saveDialog(String dialogTitle, FileFilterExtension fileFilter, String prefsKeyRecentFile) {
		String recentFile = Prefs.get(prefsKeyRecentFile, null);
        JFileChooser chooser = new JFileChooser() {
			@Override
            public void approveSelection(){
                File f = getSelectedFile();
                if(f.exists()){
                    int result = JOptionPane.showConfirmDialog(this, "Overwrite existing file?", "File exists", JOptionPane.YES_NO_CANCEL_OPTION);
                    switch(result){
                        case JOptionPane.YES_OPTION:
                            super.approveSelection();
                            return;
                        case JOptionPane.NO_OPTION:
                            return;
                        case JOptionPane.CLOSED_OPTION:
                            return;
                        case JOptionPane.CANCEL_OPTION:
                            cancelSelection();
                            return;
                    }
                }
                super.approveSelection();
            }        
        };
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setDialogType(JFileChooser.SAVE_DIALOG);
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        chooser.addChoosableFileFilter(fileFilter);
        
        if (recentFile!=null) {
        	chooser.setSelectedFile(new File(recentFile));
        	chooser.setCurrentDirectory(new File(recentFile).getParentFile());
        }
        
        chooser.setFileFilter(chooser.getChoosableFileFilters()[0]);
        chooser.setMultiSelectionEnabled(false);
        chooser.setDialogTitle(dialogTitle);
        int retvalue = chooser.showDialog(null, "Save");
        if (retvalue==JFileChooser.APPROVE_OPTION) {
        	File selectedFile = chooser.getSelectedFile();
        	if (!fileFilter.getExtension().substring(1).equals(FilenameUtils.getExtension(selectedFile.getName()))) {
        		selectedFile = new File(selectedFile.getPath()+fileFilter.getExtension());
        	}
        	return selectedFile;
        }
        return null;
	}
	
	private static abstract class FileFilterExtension extends FileFilter {
		private String extension;
		public FileFilterExtension(String extension) {
			this.extension = extension;
		}
		public String getExtension() {
			return extension;
		}
	}
	
	public static final FileFilterExtension FILE_FILTER_LOOP = new FileFilterExtension(".dimidimi") {
		@Override
		public String getDescription() { return "Single Loop (*"+getExtension()+")"; }
		@Override
		public boolean accept(File f) {	return (f.isDirectory() || f.getName().toLowerCase().endsWith(getExtension()));}
	};
	
	public static final FileFilterExtension FILE_FILTER_SESSION = new FileFilterExtension(".dimisession") {
		@Override
		public String getDescription() { return "Session (*"+getExtension()+")"; }
		@Override
		public boolean accept(File f) {	return (f.isDirectory() || f.getName().toLowerCase().endsWith(getExtension()));}
	};
}
