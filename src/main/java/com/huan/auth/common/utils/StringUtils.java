package com.huan.auth.common.utils;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;


/**
 * @desc 字符串工具类
 * @author swhuan
 */
public class StringUtils {

    public static String phoneRegex = "^((13[0-9])|(14[0-9])|(15([0-9]))|(16([0-9]))|(17[0-9])|(18[0-9])|(19[0-9]))\\d{8}$";

    /**
     * 验证手机号格式
     * @param phone
     * @return
     */
    public static boolean isValidPhone(String phone) {
        boolean valid = false;
        if (null!=phone&&!"".equals(phone)) {
            if (phone.length() == 11) {
                Pattern p = Pattern.compile(phoneRegex);
                Matcher m = p.matcher(phone);
                valid = m.matches();
            }
        }
        return valid;
    }

    public static String getFileName(String key, boolean md5) {
        if (md5) {
            return MD5(key);
        } else {
            return key;
        }
    }
    
    public static String MD5(String key) {
        char hexDigits[] = {
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
        };
        try {
            byte[] btInput = key.getBytes();
            // 获得MD5摘要算法的 MessageDigest 对象
            MessageDigest mdInst = MessageDigest.getInstance("MD5");
            // 使用指定的字节更新摘要
            mdInst.update(btInput);
            // 获得密文
            byte[] md = mdInst.digest();
            // 把密文转换成十六进制的字符串形式
            int j = md.length;
            char str[] = new char[j * 2];
            int k = 0;
            for (int i = 0; i < j; i++) {
                byte byte0 = md[i];
                str[k++] = hexDigits[byte0 >>> 4 & 0xf];
                str[k++] = hexDigits[byte0 & 0xf];
            }
            return new String(str);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获得对象的字符串形式
     * 
     * @param object 目标对象
     * @return
     */
    public static String getString(Object object) {
        return object == null ? null : object.toString();
    }

    /**
     * 获得Long值
     * 
     * @param object 目标对象
     * @return
     */
    public static Long getLong(Object object) {
        if (object instanceof Long) {
            return (Long) object;
        } else if (object instanceof Number) {
            return ((Number) object).longValue();
        } else if (object instanceof String) {
            return Double.valueOf((String) object).longValue();
        }
        return null;
    }

    /**
     * 获得Long值
     *
     * @param object 目标对象
     * @return
     */
    public static Integer getInteger(Object object) {
        if (object instanceof Integer) {
            return (Integer) object;
        } else if (object instanceof Number) {
            return ((Number) object).intValue();
        } else if (object instanceof String) {
            return Double.valueOf((String) object).intValue();
        }
        return null;
    }

    /**
     * 判断字符串是否为空
     * 
     * @param string 需要判断的字符串
     * @return
     */
    public static boolean isEmpty(String string) {
        return string == null || string.length() == 0;
    }

    /**
     * 判断实体名称是否是有效。只包含字母、数字、下划线。
     * 
     * @param str
     * @return
     */
    public static boolean isValidEntityName(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        for (int i = 0, n = str.length(); i < n; i++) {
            char c = str.charAt(i);
            if (!(c >= 65 && c <= 90) && !(c >= 97 && c <= 122) && !(c >= 48 && c <= 57) && c != '_') {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断字符串相等
     * 
     * @param str1 比较字符串1
     * @param str2 比较字符串2
     * @return
     */
    public static boolean equals(String str1, String str2) {
        return equals(false, str1, str2);
    }

    /**
     * 判断字符串相等
     * 
     * @param ignoreCase 是否忽略大小写
     * @param str1 比较字符串1
     * @param str2 比较字符串2
     * @return
     */
    public static boolean equals(boolean ignoreCase, String str1, String str2) {
        if (str1 == null) {
            return str2 == null;
        }
        return ignoreCase ? str1.equalsIgnoreCase(str2) : str1.equals(str2);
    }

    /**
     * 去除字符串中的空格
     * 
     * @param str 要去除空格的字符串
     * @return 去除空格后的字符串
     */
    public static String kickSpaces(String str) {
        StringBuilder sb = new StringBuilder();
        str.chars().filter(c -> c != ' ').forEach(c -> sb.append(c));
        return sb.toString();
    }

    /**
     * 首字母大写
     * 
     * @param string 需要转换的字符串
     * @return 首字母大写后的字符串
     */
    public static String upperFirstChar(String string) {  
        char[] charArray = string.toCharArray();
        if (charArray[0] >= 97 && charArray[0] <= 122) {
            charArray[0] -= 32;
            return String.valueOf(charArray);
        }
        return string;
    }

    /**
     * 获得一行CSV的值列表
     * 
     * @param line 行字符串
     * @return 值列表
     */
    public static List<String> getCsvCellValues(String line, Integer[] queryFieldIndexs) {
        StringBuilder tmp = new StringBuilder();
        int charLength = line.length();
        int scanIdx = 0;
        int curCol = 0;
        boolean isStart = true;
        List<String> values = new ArrayList<>();

        while (scanIdx < charLength) {
            String str = null;
            char c = line.charAt(scanIdx++);
            
            if (isStart) {
                if (c == '"') { // in quote
                    isStart = false;
                    while (scanIdx < charLength) {
                        char c1 = line.charAt(scanIdx++);
                        if (c1 == '"') {
                            if (scanIdx + 1 >= charLength) {
                                break;
                            }

                            char c2 = line.charAt(scanIdx++);
                            if (c2 == '"') { // 连续两个双引号，视为转义
                                tmp.append(c2);
                            } else if (c2 == ',') {
                                str = tmp.toString();
                                isStart = true;
                                break;
                            } else {
                                tmp.append(c2);
                                break;
                            }
                        } else {
                            tmp.append(c1);
                        }
                    }
                } else if (c == ',') {
                    str = tmp.toString();
                } else {
                    tmp.append(c);
                    isStart = false;
                }
            } else {
                if (c == ',') {
                    str = tmp.toString();
                    isStart = true;
                } else {
                    tmp.append(c);
                }
            }

            if (str != null) {
                add(str, values, queryFieldIndexs, curCol);
                curCol++;
                tmp = new StringBuilder();
            }
        }

        add(tmp.toString(), values, queryFieldIndexs, curCol);
        return values;
    }
    
    public static void add(String element, List<String> values, Integer[] queryFieldIndexs, int curCol) {
        if (queryFieldIndexs == null || queryFieldIndexs.length <= 0) {
            values.add(element);
        } else {
            boolean isQuery = Stream.of(queryFieldIndexs).filter(index -> index == curCol).findFirst().isPresent();
            if (isQuery) {
                values.add(element);
            }
        }
    }
    
    public static String getSuffix(String string){
        if (string.isEmpty()) {
            return null;
        }else {
            return string.substring(string.lastIndexOf(".") + 1);
        }
    }

    public static String getUUID() {
        UUID uuid = UUID.randomUUID();
        String str = uuid.toString();
        String uuidStr = str.replace("-", "");
        return uuidStr;
    }
}
