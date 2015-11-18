/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filelister;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystemException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 *
 * @author anthony.poon
 */
public class FileLister {
    //private static List<String> path;
    private static MoveFile worker = new MoveFile();
    private static List<String> sourcePath = new ArrayList<>();
    private static String destPath;
    public static final int ARGUMENT_MAX_SIZE = 2;
    private static CliFlag mod = null;
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        //Beware of case sensitivity. All are sensitive by design choice        
        if (args.length == 0) {
            //Man page
        } else {
            try {
                // Escape directory separator
                argsProcessing(args);
                switch (mod) {
                    case SHOW_ACL:
                        for (String pathAsString : sourcePath) {
                            ListFile listFactory = new ListFile();
                            listFactory.setSourcePath(pathAsString);
                            listFactory.output();
                            System.out.println(">>>>>>>>>>>>>>>Next File<<<<<<<<<<<<<<<");
                        }
                        break;
                    case COPY:
                        copyOperation();
                        break;
                    case MOVE:
                        moveOperation();
                        break;
                }
            } catch (ParseException | IllegalArgumentException ex) {
                ex.printStackTrace(System.out);
                System.err.println(ex.getMessage());
                System.err.println("Run the program without argument to consult man page. Exiting.");
                System.exit(-1);
            } catch (IOException ex) {
                ex.printStackTrace(System.out);
                System.err.println(ex.getMessage());
                System.err.println("Error processing files.");
                System.exit(-1);
            }
        }    
    }
    
    private static void argsProcessing(String[] args) throws ParseException, FileNotFoundException {
        //Need to check for duplication!!!!!!!!!!!!!!
        for (int i = 0; i < args.length; i++) {
            CliFlag checkMod = CliFlag.fromString(args[i]);
            
            switch (checkMod) {
                case LIST:
                    if (mod != null) {                            
                        throw new IllegalArgumentException("Duplicate incompatible flag: " + args[i] + " & " + mod.toString());
                    }
                    mod = CliFlag.LIST;
                    break;
                case MOVE:
                    if (mod != null) {
                        throw new IllegalArgumentException("Duplicate incompatible flag: " + args[i] + " & " + mod.toString());
                    }
                    mod = CliFlag.MOVE;
                    break;
                case COPY:
                    if (mod != null) {
                        throw new IllegalArgumentException("Duplicate incompatible flag: " + args[i] + " & " + mod.toString());
                    }
                    mod = CliFlag.COPY;
                    break;
                case SIMULATE:
                    worker.setIsSimulate(true);
                    break;
                case FROM_MDATE:
                    if (i == args.length - 1 || (!args[i+1].matches("\\d{8}") && (!args[i+1].matches("\\d+d")))) {
                        throw new IllegalArgumentException("Illegal parameter for " + args[i]);
                    } else {
                        // need to catach and rethrow exception later
                        if (args[i+1].matches("\\d{8}")) {
                            DateFormat formater = new SimpleDateFormat("yyyyMMdd");
                            Date date = formater.parse(args[i+1]);
                            addFromModifyDateFilter(date);
                        } else {
                            Calendar curCalendar = Calendar.getInstance();
                            curCalendar.add(Calendar.DATE, -(Integer.parseInt(args[i+1].substring(0, args[i+1].length()-1))));
                            addFromModifyDateFilter(curCalendar.getTime());
                        }
                    }
                    i = i + 1;
                    break;
                case TO_MDATE:
                    if (i == args.length - 1 || (!args[i+1].matches("\\d{8}") && (!args[i+1].matches("\\d+d")))) {
                        throw new IllegalArgumentException("Illegal parameter for " + args[i]);
                    } else {
                        // need to catach and rethrow exception later
                        if (args[i+1].matches("\\d{8}")) {
                            DateFormat formater = new SimpleDateFormat("yyyyMMdd");
                            Date date = formater.parse(args[i+1]);
                            addToModifyDateFilter(date);
                        } else {
                            Calendar curCalendar = Calendar.getInstance();
                            curCalendar.add(Calendar.DATE, -(Integer.parseInt(args[i+1].substring(0, args[i+1].length()-1))));
                            addToModifyDateFilter(curCalendar.getTime());
                        }
                    }
                    i = i + 1;
                    break;
                case FROM_CDATE:
                    if (i == args.length - 1 || (!args[i+1].matches("\\d{8}") && (!args[i+1].matches("\\d+d")))) {
                        throw new IllegalArgumentException("Illegal parameter for " + args[i]);
                    } else {
                        // need to catach and rethrow exception later
                        if (args[i+1].matches("\\d{8}")) {
                            DateFormat formater = new SimpleDateFormat("yyyyMMdd");
                            Date date = formater.parse(args[i+1]);
                            addFromCreateDateFilter(date);
                        } else {
                            Calendar curCalendar = Calendar.getInstance();
                            curCalendar.add(Calendar.DATE, -(Integer.parseInt(args[i+1].substring(0, args[i+1].length()-1))));
                            addFromCreateDateFilter(curCalendar.getTime());
                        }
                    }
                    i = i + 1;
                    break;
                case TO_CDATE:
                    if (i == args.length - 1 || (!args[i+1].matches("\\d{8}") && (!args[i+1].matches("\\d+d")))) {
                        throw new IllegalArgumentException("Illegal parameter for " + args[i]);
                    } else {
                        // need to catach and rethrow exception later
                        if (args[i+1].matches("\\d{8}")) {
                            DateFormat formater = new SimpleDateFormat("yyyyMMdd");
                            Date date = formater.parse(args[i+1]);
                            addToCreateDateFilter(date);
                        } else {
                            Calendar curCalendar = Calendar.getInstance();
                            curCalendar.add(Calendar.DATE, -(Integer.parseInt(args[i+1].substring(0, args[i+1].length()-1))));
                            addToCreateDateFilter(curCalendar.getTime());
                        }
                    }
                    i = i + 1;
                    break;
                case SHOW_ACL:
                    // Arguement Check must add later
                    mod = CliFlag.SHOW_ACL;
                    break;
                default :
                    if (!args[i].matches("^-{1,2}.*")) {
                        sourcePath.add(args[i]);
                        destPath = args[i];
                    } else {
                        throw new IllegalArgumentException("Illegal argument: " + args[i]);
                    }
                    break;
            }            
        }
        if (mod == CliFlag.COPY || mod == CliFlag.MOVE) {
            if (sourcePath.size() < 2) {
                // Must have source and dest
                // Can make automatice rename later
                throw new IllegalArgumentException("Please specify destination");
            } else {
                sourcePath.remove(sourcePath.size() - 1);
            }
        }
    }
    
    private static void addFromModifyDateFilter(final Date fDate) {
        worker.addFilter(new FileFilter() {
            @Override
            public boolean shouldMove(FilterEvent evt) {
                return evt.getAttrs().lastModifiedTime().toMillis() >= fDate.getTime();
            }

            @Override
            public String skipMessage(FilterEvent evt) {
                return "Skipped. smaller than " + CliFlag.FROM_MDATE.toString() + " (" + evt.getFilePath().normalize().toFile().getAbsolutePath() + "). Last Modify: " + evt.getAttrs().lastModifiedTime();
            }
        });        
    }
    
    private static void addToModifyDateFilter(final Date tDate) {
        worker.addFilter(new FileFilter() {
            @Override
            public boolean shouldMove(FilterEvent evt) {
                return evt.getAttrs().lastModifiedTime().toMillis() <= tDate.getTime();
            }

            @Override
            public String skipMessage(FilterEvent evt) {
                return "Skipped. smaller than " + CliFlag.FROM_MDATE.toString() + " (" + evt.getFilePath().normalize().toFile().getAbsolutePath() + "). Last Modify: " + evt.getAttrs().lastModifiedTime();
            }
        });   
    }
    
    private static void addFromCreateDateFilter(final Date FDate) {
        worker.addFilter(new FileFilter() {
            @Override
            public boolean shouldMove(FilterEvent evt) {
                return evt.getAttrs().creationTime().toMillis() >= FDate.getTime();
            }

            @Override
            public String skipMessage(FilterEvent evt) {
                return "Skipped. smaller than " + CliFlag.FROM_CDATE.toString() + " (" + evt.getFilePath().normalize().toFile().getAbsolutePath() + "). Last Modify: " + evt.getAttrs().lastModifiedTime();
            }
        });        
    }
    
    private static void addToCreateDateFilter(final Date tDate) {
        worker.addFilter(new FileFilter() {
            @Override
            public boolean shouldMove(FilterEvent evt) {
                return evt.getAttrs().creationTime().toMillis() <= tDate.getTime();
            }

            @Override
            public String skipMessage(FilterEvent evt) {
                return "Skipped. smaller than " + CliFlag.TO_CDATE.toString() + " (" + evt.getFilePath().normalize().toFile().getAbsolutePath() + "). Last Modify: " + evt.getAttrs().lastModifiedTime();
            }
        });        
    }
    
    private static void moveOperation() throws IOException{
        if (sourcePath != null && destPath != null) {
            for (String source : sourcePath) {
                worker.setSourceDestPath(source, destPath);
                worker.move();
            }
        } else {
            throw new IllegalArgumentException("Missing source or destination argument.");
        }
    }
    
    private static void copyOperation() throws IOException{
        if (sourcePath != null && destPath != null) {
            for (String source : sourcePath) {
                worker.setSourceDestPath(source, destPath);
                worker.copy();
            }
        } else {
            throw new IllegalArgumentException("Missing source or destination argument.");
        }
    }
}
