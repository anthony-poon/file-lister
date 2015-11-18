/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filelister;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryFlag;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 *
 * @author anthony.poon
 */
public class ListFile {
    class ParentAclRecord{
        private Path sourcePath;
        private AclEntry sourceAcl;
        ParentAclRecord(Path p, AclEntry acl) {
            this.sourceAcl = acl;
            this.sourcePath = p;
        }
        
        @Override
        public boolean equals(Object o) {
            ParentAclRecord castedObj = (ParentAclRecord) o;
            boolean isSourcePathEqual = sourcePath.equals(castedObj.getSourcePath());
            boolean isPermissionEqual = sourceAcl.permissions().equals(castedObj.getAcl().permissions());
            boolean isPrincipleEqual = sourceAcl.principal().equals(castedObj.getAcl().principal());
            return ((isSourcePathEqual && isPrincipleEqual) && isPermissionEqual);
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 29 * hash + Objects.hashCode(this.sourcePath);
            hash = 29 * hash + Objects.hashCode(this.sourceAcl.permissions());
            hash = 29 * hash + Objects.hashCode(this.sourceAcl.principal());
            return hash;
        }
        
        public Path getSourcePath() {
            return sourcePath;
        }
        
        public AclEntry getAcl() {
            return sourceAcl;
        }
        
        public void setSourcePath(Path p) {
            this.sourcePath = p;
        }
        public void setAcl(AclEntry acl) {
            this.sourceAcl = acl;
        }
    }
    private static final List<String> permissionOrder = new ArrayList<>(Arrays.asList(
            new String[]{
                "EXECUTE",
                "READ_DATA",
                "READ_ATTRIBUTES",
                "READ_NAMED_ATTRS",
                "WRITE_DATA",
                "APPEND_DATA",
                "WRITE_ATTRIBUTES",
                "WRITE_NAMED_ATTRS",
                "DELETE_CHILD",
                "DELETE",
                "READ_ACL",
                "WRITE_ACL",
                "WRITE_OWNER",
                "SYNCHRONIZE"
            }
    ));
    private FileTree tree = new FileTree(null);
    private static final boolean isSkipBuiltIn = true;
    private Path sourcePath;
    private AclFileAttributeView aclView;
    private List<AclEntry> aclEntryList;
    private Map<ParentAclRecord, Set<AclEntry>> aclMap = new LinkedHashMap<>();
    public void setSourcePath(String sourcePath) throws FileNotFoundException {
        this.sourcePath = Paths.get(sourcePath);
        if (!Files.exists(this.sourcePath, LinkOption.NOFOLLOW_LINKS)) {
            throw new FileNotFoundException("Source File not found");
        }        
    }
    class ListVisitor extends SimpleFileVisitor<Path> {

        @Override
        public FileVisitResult postVisitDirectory(Path path, IOException ioe) throws IOException {
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path path, BasicFileAttributes bfa) throws IOException {
            //System.out.println("Files: " + path.toAbsolutePath());
            //printAclDetail(path);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes bfa) throws IOException {
            String relativePath = sourcePath.toAbsolutePath().relativize(path.toAbsolutePath()).toString();
            int depth = relativePath.length() - relativePath.replace("\\", "").length();
            // 0 depth = immediate child <-- need to fix?
            tree.getRoot().addChild(path);

            if (depth == 0) {
                System.out.println("Folders: " + path.toAbsolutePath());
                processNodeAcl(path);
                return FileVisitResult.CONTINUE;
            } else {
                return FileVisitResult.SKIP_SUBTREE;
            }
        }     
    }
    
    public void output(){
        try {
            Files.walkFileTree(sourcePath, new ListVisitor());
        } catch (IOException ex){
            System.err.println("Error: " + ex.getMessage());
        }
        System.out.println();
        System.out.println("**************************");
        for (Map.Entry<ParentAclRecord, Set<AclEntry>> entry : aclMap.entrySet()){            
            if (!entry.getValue().isEmpty()) {
                System.out.println("Entry Folder: " + entry.getKey().getSourcePath());
                for (AclEntry value : entry.getValue()) {
                    aclEntryPrinter(value);
                }
            }
        }
    }
    
    private void processNodeAcl(Path path) throws IOException {
        /**
         * READ_NAMED_ATTRS = Read Extended Attributes
         * WRITE_NAMED_ATTRS = Write Extended Attributes
         * APPEND_DATA = Create Folder / Apprehend Data
         * READ_ACL = Read Permission
         * WRITE_OWNER = Take ownership
         * DELETE_CHILD = Delete sub folder and files
         * SYNCHRONIZE = ?
         * WRITE_DATA = Create File / Write Data
         * WRITE_ATTRIBUTES = Write Attribute
         * READ_DATA = List folders / read data
         * DELETE = Delete
         * WRITE_ACL = Change Permission
         * READ_ATTRIBUTES = Read Attributes
         * EXECUTE = Traverse folders / Execute file 
         * MS KB: https://support.microsoft.com/en-us/kb/308419
         */        
        aclView = Files.getFileAttributeView(path, AclFileAttributeView.class);
        aclEntryList = aclView.getAcl();
        for (AclEntry acl : aclEntryList) {
            boolean shouldSkip = false;
            if (isSkipBuiltIn) {
                shouldSkip = !acl.principal().getName().matches("^ASIAMINERALS\\\\[^\"/\\\\\\[\\]:;|=,+*?<>]+");
            }
            if (!shouldSkip) {
                //System.out.println("Processing the following ACL rules:");
                //aclEntryPrinter(acl);
                ParentAclRecord aclOriginPath = getAclOrigin(new ParentAclRecord(path, acl));
                if (aclMap.containsKey(aclOriginPath)) {                    
                    Set<AclEntry> existingSet = aclMap.get(aclOriginPath);
                    if (!isInherited(aclOriginPath.getSourcePath(), acl)){
                        existingSet.add(acl);
                    };
                } else {
                    HashSet<AclEntry> aclSet = new HashSet<>();
                    aclSet.add(acl);
                    aclMap.put(aclOriginPath, aclSet);
                }
            }
        }
    }
    
    private ParentAclRecord getAclOrigin(ParentAclRecord testingRecord) throws IOException {
        Path parent = testingRecord.getSourcePath().getParent();
        if (parent == null) {
            return testingRecord;
        } else {
            if (!isInherited(parent, testingRecord.getAcl())) {
                return testingRecord;
            } else {
                testingRecord.setSourcePath(parent);
                return getAclOrigin(testingRecord);
            }
        }
    }
    
    private void aclEntryPrinter(AclEntry entry) {
        System.out.print("\t");
        System.out.println("User Principal Name: " + entry.principal().getName());
        System.out.print("\t");
        System.out.println("ACL Entry Type: " + entry.type());
        Set<AclEntryFlag> flagsSet = entry.flags();
        if (!flagsSet.isEmpty()) {
            int i = 1;
            for (AclEntryFlag flag : flagsSet) {
                System.out.print("\t\t");
                System.out.print("Flag " + i++ + ": ");
                System.out.println(flag.name());
            }
        } else {
            System.out.print("\t\t");
            System.out.println("No Flag.");
        }

        Set<AclEntryPermission> permissionSet = entry.permissions();
        if (!permissionSet.isEmpty()) {
            if (permissionSet.size() == AclEntryPermission.values().length) {
                System.out.print("\t\t");
                System.out.println("Full Permissions.");
            } else {
                printPermission(permissionSet);
            }
        } else {
            System.out.print("\t\t");
            System.out.println("No Permission.");
        }        
        System.out.println();
    }
    
    private boolean isInherited(Path testingParent, AclEntry acl) throws IOException {
        List<AclEntry> parentAclSet = Files.getFileAttributeView(testingParent, AclFileAttributeView.class).getAcl();
        for (AclEntry parentAcl : parentAclSet) {
            if (parentAcl.permissions().equals(acl.permissions()) && parentAcl.principal().equals(acl.principal())) {
                return true;
            }
        }
        return false;
    }
    
    private void printPermission(Set<AclEntryPermission> permissionSet) {        
        Comparator<AclEntryPermission> sorter = new Comparator<AclEntryPermission>() {
            @Override
            public int compare(AclEntryPermission t, AclEntryPermission t1) {
                return permissionOrder.indexOf(t.name()) - permissionOrder.indexOf(t1.name());
            }
        };
        TreeSet<AclEntryPermission> sortedPermission = new TreeSet<>(sorter);

        for (AclEntryPermission permission : permissionSet) {              
            sortedPermission.add(permission);
        }
        int i = 1;
        for (AclEntryPermission permission : sortedPermission) {
            System.out.print("\t\t");
            System.out.print("Permissions " + i++ + ": ");
            String outputStr = permission.name();
            switch (outputStr) {
                case "READ_NAMED_ATTRS":
                    outputStr += " (Read Extended Attributes)";
                    break;
                case "WRITE_NAMED_ATTRS":
                    outputStr += " (Write Extended Attributes)";
                    break;
                case "APPEND_DATA":
                    outputStr += " (Create Folder / Apprehend Data)";
                    break;
                case "READ_ACL":
                    outputStr += " (Read Permission)";
                    break;
                case "WRITE_OWNER":
                    outputStr += " (Take ownership)";
                    break;
                case "DELETE_CHILD":
                    outputStr += " (Delete sub folder and files)";
                    break;
                case "WRITE_DATA":
                    outputStr += " (Create File / Write Data)";
                    break;
                case "WRITE_ATTRIBUTES":
                    outputStr += " (Write Attribute)";
                    break;
                case "READ_DATA":
                    outputStr += " (List folders / read data)";
                    break; 
                case "DELETE":
                    outputStr += " (Delete)";
                    break; 
                case "WRITE_ACL":
                    outputStr += " (Change Permission)";
                    break; 
                case "READ_ATTRIBUTES":
                    outputStr += " (Read Attributes)";
                    break; 
                case "EXECUTE":
                    outputStr += " (Traverse folders / Execute file)";
                    break; 
            }
            System.out.println(outputStr);
        }
    }
}
