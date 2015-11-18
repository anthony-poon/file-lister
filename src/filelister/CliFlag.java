/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filelister;

/**
 *
 * @author anthony.poon
 */
public enum CliFlag {
    FROM_MDATE("--mdatefrom"),
    TO_MDATE("--mdateto"),
    MOVE("-m"),
    COPY("-c"),
    SIMULATE("-s"),
    LIST("-l"),
    FROM_CDATE("--cdatefrom"),
    TO_CDATE("--cdateto"),
    SHOW_ACL("--acl"),
    OTHER("");
    private String flag;
    
    CliFlag(String str) {
        this.flag = str;
    }
    
    @Override
    public String toString() {
        return flag;
    }
    
    public static boolean contains(String str) {
        if (str.matches("^-{1,2}.+")) {
            for (CliFlag enumValue : CliFlag.values()) {
                if (enumValue.toString().equals(str)) {
                    return true;
                }
            }
            throw new IllegalArgumentException("Invalid Flag : " + str);
        }
        return false;
    }
    
    public static CliFlag fromString(String str) {
        for (CliFlag enumValue : CliFlag.values()) {
            if (str.equals(enumValue.toString())) {
                return enumValue;
            }
        }
        return CliFlag.OTHER;
    }
}
