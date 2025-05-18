package commands;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataSet {
    public static int DATASET_RECORD = 0;
    public static int DATASET_SEQUENCE = 1;
    public static int DATASET_LIST = 2;
    private int seqFrom;
    private int seqTo;
    private int seqStep;
    private String idName;

    private final int dataKind;
    ResultSetMetaData metaData;
    public List<String[]> rowData;
    public HashMap<String,Integer> columns;
    public boolean isEmpty(){
        return rowData.isEmpty();
    }
    public int getRowCount() {
        if(DATASET_RECORD == dataKind || DATASET_LIST == dataKind)
            return rowData.size();
        else if(DATASET_SEQUENCE == dataKind){
                if((seqStep > 0 && seqFrom > seqTo) || (seqStep < 0 && seqFrom < seqTo)) return 0;
                return 1 + (seqTo - seqFrom) / seqStep;
        }else return 0;
    }
    public DataSet(List<String> list, String id) {
        dataKind = DATASET_LIST;
        idName = id;
        rowData = new ArrayList<>();
        for(String s: list) {
            rowData.add(new String[]{s});
        }
    }
    public DataSet(int from, int to, int step, String id){
        seqFrom = from;
        seqTo = to;
        seqStep = step;
        idName = id;
        dataKind = DATASET_SEQUENCE;
    }
    public Map<String, String> mapCursorValue(int index){
        HashMap<String, String> map = new HashMap<>();
        if(DATASET_RECORD == dataKind) {
            for(Map.Entry<String, Integer> e:columns.entrySet()) {
                map.put("\\{" + e.getKey() + "\\}", rowData.get(index - 1)[e.getValue()]);
            }
        }else if(DATASET_SEQUENCE == dataKind) {
           map.put("\\{" + idName + "\\}",index + "");
        }else if(DATASET_LIST == dataKind){
           map.put("\\{" + idName + "\\}",rowData.get(index - 1)[0]);
        }
        return map;
    }
    public DataSet(ResultSet rs) {
        try {
            dataKind = DATASET_RECORD;
            metaData = rs.getMetaData();
            rowData = new ArrayList<>();
            columns = new HashMap<>();
            int cols = metaData.getColumnCount();
            for(int i = 1; i<= cols; i++) {
                columns.put(metaData.getColumnLabel(i).toLowerCase(),i-1);
            }
            while(rs.next()) {
                String[] row = new String[cols];
                for(int i=1; i <= cols; i++) {
                    row[i-1] = rs.getString(i);
                    if(row[i-1] == null) row[i-1] = "";
                }
                rowData.add(row);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    private boolean isChinese(char c) {
        return c >= '\u4e00' && c <= '\u9fff'; // 判断是否为汉字
    }
    private int calculateDisplayWidth(String str) {
        int width = 0;
        for (char c : str.toCharArray()) {
            width += isChinese(c) ? 2 : 1;
        }
        return width;
    }
    private String repeat(String src, int times) {
        StringBuilder sb = new StringBuilder(src.length() * times);
        for(int i = 1; i <= times; i++) {
            sb.append(src);
        }
        return sb.toString();
    }
    public void displayDataSet() {
        if(metaData == null) return;
        int i;
        int cols = getColumnCount();
        int[] maxLength = new int[cols];

        for(i = 0; i < cols; i++) {
            try {
                maxLength[i] = calculateDisplayWidth(metaData.getColumnLabel(i+1));
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        for (String[] x : rowData) {
            for (i = 0; i < x.length; i++) {
                int len = calculateDisplayWidth(x[i]);
                if(len > maxLength[i]) {
                    maxLength[i] = len;
                }
            }
        }

        try {
            StringBuilder sb = new StringBuilder();
            sb.append("+");
            for(i = 0; i < maxLength.length; i++) {
                sb.append(repeat("-",maxLength[i] + 2)).append("+");
            }
            sb.append("\n").append("|");
            for(i = 1; i <= cols; i++) {
                String s = metaData.getColumnLabel(i);
                sb.append(" ").append(String.format("%-" + (s.length() +  maxLength[i-1] - calculateDisplayWidth(s)) +"s", s)).append(" |");
            }
            sb.append("\n").append("+");
            for(i = 0; i < maxLength.length; i++) {
                sb.append(repeat("-",maxLength[i] + 2)).append("+");
            }
            System.out.println(sb); //print the header of result
            for (String[] x : rowData) {
                sb.setLength(0);
                sb.append("|");
                for (i = 0; i < x.length; i++) {
                    int colType =  metaData.getColumnType(i+1);
                    int len = x[i].length() + maxLength[i] - calculateDisplayWidth(x[i]);
                    if(colType == Types.CHAR || colType == Types.VARCHAR || colType == Types.LONGVARCHAR) {
                        sb.append(" ").append(String.format("%-" + len +"s",x[i])).append(" |");
                    } else {
                        sb.append(" ").append(String.format("%" + len +"s",x[i])).append(" |");
                    }
                }
                System.out.println(sb);
            }
            if(!rowData.isEmpty()) {
                sb.setLength(0);
                sb.append("+");
                for(i = 0; i < maxLength.length; i++) {
                    sb.append(repeat("-",maxLength[i] + 2)).append("+");
                }
                System.out.println(sb);
            }
        } catch (Exception ignored) {

        }
    }
    private int getColumnCount() {
        try {
            return metaData.getColumnCount();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int getStep() {
        if(DATASET_RECORD == dataKind || DATASET_LIST == dataKind) {
            return 1;
        }
        return seqStep;
    }

    public int getInitialRow() {
        if(DATASET_RECORD == dataKind || DATASET_LIST == dataKind) {
            return 0;
        }
        return seqFrom - seqStep;
    }

    public boolean isValidRow(int row) {
        if(DATASET_RECORD == dataKind || DATASET_LIST == dataKind) {
            return row <= rowData.size();
        }
        if(seqStep > 0) return row <= seqTo;
        return row >= seqTo;
    }
}
