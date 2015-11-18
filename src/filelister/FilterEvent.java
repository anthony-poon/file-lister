/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filelister;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

/**
 *
 * @author anthony.poon
 */
public class FilterEvent {
    private Path filePath;
    private BasicFileAttributes attrs;

    public Path getFilePath() {
        return filePath;
    }

    public void setFilePath(Path filePath) {
        this.filePath = filePath;
    }

    public BasicFileAttributes getAttrs() {
        return attrs;
    }

    public void setAttrs(BasicFileAttributes attrs) {
        this.attrs = attrs;
    }
    
}
