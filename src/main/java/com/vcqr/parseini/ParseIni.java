package com.vcqr.parseini;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

public class ParseIni {
    public String defaultName = "default";
    private String sectionName;
    public HashMap<String, Object> sections = new HashMap<String, Object>();
    private HashMap<String, Object> property = new HashMap<String, Object>();
    private HashMap<String, Object> parentMap;

    /**
     * 构造函数
     * 
     * @param filename
     *            文件路径
     * @throws IOException
     */
    public ParseIni(String filename) throws IOException {
        sectionName = this.defaultName;
        sections.put(sectionName, property);
        read(filename);
    }

    /**
     * 文件读取
     * @param filename 
     * 
     * @param reader
     * @throws IOException
     */
    protected void read(String filename) throws IOException {
        String line;
        BufferedReader reader = new BufferedReader(new FileReader(filename));

        while ((line = reader.readLine()) != null) {
            parseLine(line);
        }
        
        reader.close();
    }

    /**
     * 解析每行数据
     * 
     * @param line
     */
    @SuppressWarnings("unchecked")
    protected void parseLine(String line) {
        line = line.trim();
        if (line.indexOf('#') == 0 || line.indexOf(';') == 0) {
            return;
        }

        //处理section
        if (line.matches("\\[.*\\]")) {
            sectionName = line.replaceFirst("\\[(.*)\\]", "$1").trim();
            property = new HashMap<String, Object>();
            if (sectionName.matches(".*:.*")) {
                int pos = sectionName.indexOf(':');
                String child = sectionName.substring(0, pos).trim();
                String parent = sectionName.substring(pos + 1).trim();
                if(child.equals(parent) || child == parent){
                    return;
                }

                parentMap = this.getSection(parent);
                if (parentMap != null) {
                    // 继承父sections
                    property = (HashMap<String, Object>) parentMap.clone();
                    sections.put(child, property);
                }
            } else {
                sections.put(sectionName, property);
            }
        } else if (line.matches(".*=.*")) { //处理键值对
            int i = line.indexOf('=');
            String name = line.substring(0, i).trim();
            String value = line.substring(i + 1).trim();

            if (value.indexOf('"') == 0 || value.indexOf('\'') == 0) {
                // 去掉前面符号 " 或 '
                value = value.substring(1, value.length());
                // 去掉后面 " 或 '
                int len = value.length();
                if (value.indexOf('"') == len - 1 || value.indexOf('\'') == len - 1) {
                    value = value.substring(0, len - 1);
                }
            }

            this.setProperty(name, value);
        }
    }

    /**
     * 设置 Property
     * 
     * @param key
     * @param value
     */
    public void setProperty(String key, String value) {
        if (key.matches(".*\\..*")) {
            String[] keyArr = key.split("\\.");
            //替换串
            String tempStr = "";
            
            int keyArrLen = keyArr.length;
            int i = keyArrLen - 1;
            do{
                tempStr = keyArr[i] + "." + tempStr;
                
                if (i == keyArrLen - 1) {
                    //剔除后面的"."
                    tempStr = tempStr.substring(0, tempStr.length() - 1);
                    //获取前面key
                    String prevStr = key.replace("."+tempStr, "");
                    
                    this.setKeyVal(prevStr, keyArr[i], value);
                    // 设置整串索引值
                    property.put(key, value);
                } else {
                    // 获取前面key
                    String prevStr = key.replace("."+tempStr, "");
                    String currentStr = prevStr + "." + keyArr[i];
                    this.setKeyVal(prevStr, currentStr, property.get(currentStr));
                }
                
                --i;
            } while(i > 0);
        } else {
            property.put(key, value);
        }
    }

    /**
     * 设置键与值
     * 
     * @param prevkeyStr
     * @param currentStr
     * @param value
     */
    @SuppressWarnings("unchecked")
    public void setKeyVal(String prevkeyStr, String currentStr, Object value) {
        HashMap<String, Object> tempMap = (HashMap<String, Object>) property.get(prevkeyStr);

        if (tempMap == null || tempMap.equals(null)) {
            HashMap<String, Object> valMap = new HashMap<String, Object>();
            valMap.put(currentStr, value);
            property.put(prevkeyStr, valMap);
        } else {
            tempMap.put(currentStr, value);
            property.put(prevkeyStr, tempMap);
        }
    }

    /**
     * 获取名字
     * 
     * @param strArr
     * @param pos
     * @return
     */
    public String getStrName(String[] strArr, int pos) {
        if (pos < 0) {
            return "";
        }

        String strName = "";
        do {
            strName = strArr[pos] + "." + strName;
            pos--;
        } while (pos >= 0);

        return strName.substring(0, strName.length() - 1);
    }

    /**
     * 获取节下所有key
     * 
     * @param section
     * @return Properties
     */
    @SuppressWarnings("unchecked")
    public HashMap<String, Object> getSection(String section) {
        if (section == "" || section.equals(null))
            section = this.defaultName;

        HashMap<String, Object> property = (HashMap<String, Object>) sections.get(section);
        if (property == null) {
            sections.put(section, property);
        }

        return property;
    }

    /**
     * 根据节 和 key 获取值
     * 
     * @param section
     * @param key
     * @return String
     */
    @SuppressWarnings("unchecked")
    public Object get(String key, String section) {
        if (section == "" || section.equals(null))
            section = this.defaultName;

        HashMap<String, Object> property = (HashMap<String, Object>) sections.get(section);
        if (property == null || property.equals(null)) {
            return null;
        }

        Object value = property.get(key);
        if (value == null || value.equals(null))
            return null;

        return value;
    }

    /**
     * 获取默认节点内容
     * 
     * @param key
     * @return
     */
    public Object get(String key) {
        return this.get(key, "");
    }

    /**
     * 设置值
     * 
     * @param section
     * @param key
     * @param val
     */
    @SuppressWarnings("unchecked")
    public void set(String section, String key, String val) {
        if (section.equals(null) || section == "")
            section = this.defaultName;
        
        property = (HashMap<String, Object>) sections.get(section);
        if (property == null || property.equals(null))
            property = new HashMap<String, Object>();

        if (key == "" || key.equals(null)) {
            System.out.println("key is null");
            return;
        }

        sections.put(section, property);
        this.setProperty(key, val);
    }

    /**
     * 设置值
     * 
     * @param key
     * @param val
     */
    public void set(String key, String val) {
        this.set("", key, val);
    }
}
