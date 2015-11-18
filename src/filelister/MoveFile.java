/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filelister;

import java.nio.file.SimpleFileVisitor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemLoopException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author anthony.poon
 */



public class MoveFile {
    
    private Path sourcePath;
    
    private Path destPath;
    
    private boolean isSimulate = false;
    public void setIsSimulate(boolean isSimulate) {
        this.isSimulate = isSimulate;
    }    
    private final List<FileFilter> filterList = new ArrayList<>();
    
    class MoveVisitor extends SimpleFileVisitor<Path> {
        
        private final Path destPath;
        private final Map<Path, Boolean> isChanged = new LinkedHashMap<>();
        
        // Need to add option to preserve attrs/overwrite  
        
        MoveVisitor (Path destPath) {
            this.destPath = destPath;
        }
        
        @Override        
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            // Copy the directory before anything
            CopyOption[] options = new CopyOption[] {
                StandardCopyOption.COPY_ATTRIBUTES
            };
            
            // sourcePath.relativize(dir) = relative path
            // destPath.resolve will create the new path using the relative path
            
            Path newDir = destPath.resolve(sourcePath.relativize(dir));     
            try {
                // When a subfolder is added to a folder, the action will change the modify time
                // postVisitDirectory will fix the problem
                isChanged.put(dir, false);
                System.out.println(dir.normalize().toAbsolutePath().toString() + " -> " + newDir.normalize().toAbsolutePath().toString());
                if (!isSimulate) {
                    Files.copy(dir, newDir, options);
                }
            } catch (FileAlreadyExistsException ex) {
                // Ignore
            } catch (IOException ex) {
                System.out.println("Unable to create folder " + dir.toFile().getAbsolutePath() + "(" + ex + ")");
                return FileVisitResult.SKIP_SUBTREE;
            }
            return FileVisitResult.CONTINUE;
        }
        
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            // Copy Files, not folder
            CopyOption[] options = new CopyOption[] {
                StandardCopyOption.COPY_ATTRIBUTES,
                StandardCopyOption.REPLACE_EXISTING
            };
            try {
                if (filterList.isEmpty()) {
                    System.out.println(file.normalize().toAbsolutePath().toString() + " -> " + destPath.resolve(sourcePath.relativize(file)).normalize().toAbsolutePath().toString());
                    isChanged.put(file.getParent(), Boolean.TRUE);
                    if (!isSimulate) {
                        Files.move(file, destPath.resolve(sourcePath.relativize(file)), options);
                    }
                } else {
                    for (FileFilter filter : filterList) {
                        //Calling file against filter
                        FilterEvent moveEvent = new FilterEvent();
                        moveEvent.setFilePath(file);
                        moveEvent.setAttrs(attrs);
                        if (filter.shouldMove(moveEvent)) {
                            System.out.println(file.normalize().toAbsolutePath().toString() + " -> " + destPath.resolve(sourcePath.relativize(file)).normalize().toAbsolutePath().toString());
                            isChanged.put(file.getParent(), Boolean.TRUE);
                            if (!isSimulate) {
                                Files.move(file, destPath.resolve(sourcePath.relativize(file)), options);
                            }
                        } else {
                            String msg = filter.skipMessage(moveEvent);
                            if (msg != null) {
                                System.out.println(msg);
                            }
                        }
                    }
                }
            } catch (IOException ex) {
                System.out.println("Unable to create files :" + file.toFile().getAbsolutePath() + "(" + ex + ")");
            }
            return FileVisitResult.CONTINUE;
        }
        
        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            // Fix up modification time of directory when done
            if (isChanged.get(dir).equals(Boolean.TRUE)) {
                if (!isSimulate) {
                    fixLastModifyTime(dir);
                }
            } else {  
                for (FileFilter filter : filterList) {
                    FilterEvent moveEvent = new FilterEvent();
                    moveEvent.setFilePath(dir);
                    moveEvent.setAttrs(Files.readAttributes(dir, BasicFileAttributes.class));
                    if (filter.shouldMove(moveEvent)) {
                        if (!isSimulate) {
                            fixLastModifyTime(dir);
                        }
                    } else {
                        try {
                            if (!isSimulate) {
                                Files.delete(dir);
                            }
                            System.out.println(dir.normalize().toAbsolutePath().toString() + " deleted. Because empty and not matching filter rules.");
                        } catch (IOException ex) {
                            System.out.println("Error: " + ex.getMessage());                            
                        }
                    }
                } 
            }
            return FileVisitResult.CONTINUE;
        }
        
        private void fixLastModifyTime(Path dir) {
            Path newdir = destPath.resolve(sourcePath.relativize(dir));
            try {
                FileTime time = Files.getLastModifiedTime(dir);
                Files.setLastModifiedTime(newdir, time);
            } catch (IOException ex) {
                System.out.println("Unable to copy last modify time :" + dir.toFile().getAbsolutePath() + "(" + ex + ")");
            }
        }
        
        @Override
        public FileVisitResult visitFileFailed(Path file, IOException ex) {
            if (ex instanceof FileSystemLoopException) {
                System.err.println("Circular reference detected: " + file);
            } else {
                System.out.println("Unable to create files :" + file.toFile().getAbsolutePath() + "(" + ex + ")");
            }
            return FileVisitResult.CONTINUE;
        }
    }
    
    class CopyVisitor extends SimpleFileVisitor<Path> {
        
        private Path destPath;
        private Map<Path, Boolean> isChanged = new LinkedHashMap<>();
        
        // Need to add option to preserve attrs/overwrite        
        CopyVisitor (Path destPath) {
            this.destPath = destPath;
        }
        
        @Override        
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            // Copy the directory before anything
            CopyOption[] options = new CopyOption[] {
                StandardCopyOption.COPY_ATTRIBUTES
            };
            
            // sourcePath.relativize(dir) = relative path
            // destPath.resolve will create the new path using the relative path
            Path newDir = destPath.resolve(sourcePath.relativize(dir));     
            try {
                // When a subfolder is added to a folder, the action will change the modify time
                // postVisitDirectory will fix the problem
                isChanged.put(dir, false);
                System.out.println(dir.normalize().toAbsolutePath().toString() + " -> " + newDir.normalize().toAbsolutePath().toString());
                if (!isSimulate) {
                    Files.copy(dir, newDir, options);
                }
            } catch (FileAlreadyExistsException ex) {
                // Ignore
            } catch (IOException ex) {
                System.out.println("Unable to create folder " + dir.toFile().getAbsolutePath() + "(" + ex + ")");
                return FileVisitResult.SKIP_SUBTREE;
            }
            return FileVisitResult.CONTINUE;
        }
        
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            // Copy Files, not folder
            CopyOption[] options = new CopyOption[] {
                //StandardCopyOption.COPY_ATTRIBUTES,
                StandardCopyOption.REPLACE_EXISTING
            };
            try {
                if (filterList.isEmpty()) {
                    System.out.println(file.normalize().toAbsolutePath().toString() + " -> " + destPath.resolve(sourcePath.relativize(file)).normalize().toAbsolutePath().toString());
                    isChanged.put(file.getParent(), Boolean.TRUE);
                    if (!isSimulate) {
                        Files.copy(file, destPath.resolve(sourcePath.relativize(file)), options);
                    }
                } else {
                    for (FileFilter filter : filterList) {
                        //Calling file against filter
                        FilterEvent copyEvent = new FilterEvent();
                        copyEvent.setFilePath(file);
                        copyEvent.setAttrs(attrs);
                        if (filter.shouldMove(copyEvent)) {
                            System.out.println(file.normalize().toAbsolutePath().toString() + " -> " + destPath.resolve(sourcePath.relativize(file)).normalize().toAbsolutePath().toString());
                            isChanged.put(file.getParent(), Boolean.TRUE);
                            if (!isSimulate) {
                                Files.copy(file, destPath.resolve(sourcePath.relativize(file)), options);
                            }
                        } else {
                            String msg = filter.skipMessage(copyEvent);
                            if (msg != null) {
                                System.out.println(msg);
                            }
                        }
                    }
                }
            } catch (IOException ex) {
                System.out.println("Unable to create files :" + file.toFile().getAbsolutePath() + "(" + ex + ")");
            }
            return FileVisitResult.CONTINUE;
        }
        
        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            // Fix up modification time of directory when done
            if (!isSimulate) {
                if (filterList.isEmpty() || isChanged.get(dir) == Boolean.TRUE || destPath.resolve(sourcePath.relativize(dir)).toFile().list().length != 0) {
                        fixLastModifyTime(dir);

                } else {
                    // Delete destination folder if : 1. dest folder is empty 2. The folder itself should not be moved. 3. file filter applied 
                    for (FileFilter filter : filterList) {
                        FilterEvent copyEvent = new FilterEvent();
                        copyEvent.setFilePath(dir);
                        copyEvent.setAttrs(Files.readAttributes(dir, BasicFileAttributes.class));
                        if (filter.shouldMove(copyEvent)) {
                            fixLastModifyTime(dir);
                            if (dir.toFile().list().length == 0 && isChanged.containsKey(dir)) {
                                try {
                                    Files.delete(dir);
                                } catch (IOException ex) {
                                    System.out.println("Error deleting : " + ex.getMessage()); 
                                }
                            }
                        } else {
                            try {
                                Files.delete(destPath.resolve(sourcePath.relativize(dir)));
                                System.out.println(destPath.resolve(sourcePath.relativize(dir)).normalize().toAbsolutePath().toString() + " deleted. Because empty and not matching filter rules.");
                            } catch (IOException ex) {
                                System.out.println("Error deleting : " + ex.getMessage());                            
                            }
                        }
                    }               
                }
            }
            return FileVisitResult.CONTINUE;
        }
        
        private void fixLastModifyTime(Path dir) {
            Path newdir = destPath.resolve(sourcePath.relativize(dir));
            try {
                FileTime time = Files.getLastModifiedTime(dir);
                Files.setLastModifiedTime(newdir, time);
            } catch (IOException ex) {
                System.out.println("Unable to copy last modify time :" + dir.toFile().getAbsolutePath() + "(" + ex + ")");
            }
        }
        
        @Override
        public FileVisitResult visitFileFailed(Path file, IOException ex) {
            if (ex instanceof FileSystemLoopException) {
                System.err.println("Circular reference detected: " + file);
            } else {
                System.out.println("Unable to create files :" + file.toFile().getAbsolutePath() + "(" + ex + ")");
            }
            return FileVisitResult.CONTINUE;
        }
    }
    
    
    public void setSourceDestPath(String sourcePath, String destPath) throws FileNotFoundException {
        this.sourcePath = Paths.get(sourcePath);
        if (!Files.exists(this.sourcePath, LinkOption.NOFOLLOW_LINKS)) {
            throw new FileNotFoundException("Source File not found");
        }
        if (Files.isDirectory(this.sourcePath, LinkOption.NOFOLLOW_LINKS)) {
            this.destPath = Paths.get(destPath);
        } else {
            this.destPath = Paths.get(destPath).toAbsolutePath().resolve(this.sourcePath.getFileName());
        }
    }
    public MoveFile() {
    }
    
    public void addFilter (FileFilter filter) {
        filterList.add(filter);
    }
    
    public void move() throws IOException {
        if (destPath != null && sourcePath!= null) {
            CopyOption[] options = new CopyOption[] {
                StandardCopyOption.REPLACE_EXISTING
            };

            MoveVisitor mVistor = new MoveVisitor(destPath);
            Files.walkFileTree(sourcePath, mVistor);
        } else {
            throw new IllegalArgumentException("Missing source or destination argument.");
        }
    }
    
    public void copy() throws IOException {
        if (destPath != null && sourcePath!= null) {
            CopyOption[] options = new CopyOption[] {
                StandardCopyOption.COPY_ATTRIBUTES,
                StandardCopyOption.REPLACE_EXISTING
            };

            CopyVisitor cVistor = new CopyVisitor(destPath);
            Files.walkFileTree(sourcePath, cVistor);
        } else {
            throw new IllegalArgumentException("Missing source or destination argument.");
        }
    }
}
