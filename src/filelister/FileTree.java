package filelister;


import java.nio.file.Path;
import java.nio.file.attribute.AclEntry;
import java.util.ArrayList;
import java.util.List;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author anthony.poon
 */
public class FileTree {
    private FileNode root;
    class FileNode {
        private Path path;
        private List<FileNode> childNode = new ArrayList<>();
        private FileNode parent;
        private List<AclEntry> aclList = new ArrayList<>();
        FileNode(FileNode p, Path path) {
            parent = p;
        }
        
        public FileNode getParent() {
            return parent;
        }
        
        public void addChild(Path path){
            childNode.add(new FileNode(this, path));
        }
        
        public List<FileNode> getChild(){
            return childNode;
        }
        
        public boolean addAcl(AclEntry acl) {
            aclList.add(acl);
            return true;
        }
    }
    
    FileTree(Path path) {
        root = new FileNode(null, path);
    }
    
    public FileNode getRoot() {
        return root;
    }
}
