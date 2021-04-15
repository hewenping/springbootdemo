package com.nasus.websocket.controller;

/**
 * @PACKAGE_NAME: com.nasus.websocket.controller
 * @company:北京恩洪教育公司
 * @USER:小清新
 * @date: 2021年01月28日 13:13
 * @PROJECT_NAME: websocket
 **/
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

public class SudokuGame extends JFrame implements DocumentListener {

    private static final long serialVersionUID = 1L;
    private JPanel[] pnlGame;
    private JTextField[][][] txtGame;
    private Map<JTextField, JTextField> warnFiledMap = new HashMap<JTextField, JTextField>();
    private Document doc;
    private static final int YES = 1;// 单元格可取值存在 ; 已回滚;已冲突
    private static final int NO = 0;// 单元格可取值不存在 ;未回滚;未冲突
    private static final int NEED_ROLLBACK = 10;// 需要回滚
    private int is_num = YES; //输入的是不是数字标记
    private int is_remove_by_insert = NO;//remove是不是在已有值的前提下insert一个新值导致的

    private Grid[][][] grids = new Grid[9][3][3];

    public SudokuGame() {
        pnlGame = new JPanel[9];
        txtGame = new JTextField[9][3][3];
        gameInit();
    }

    /**
     * 游戏初始化
     */
    @SuppressWarnings("static-access")
    public void gameInit() {
        this.setDefaultCloseOperation(this.EXIT_ON_CLOSE);
        this.setSize(300, 300);
        this.setResizable(false);
        this.setTitle("数独游戏");
        this.setLayout(new GridLayout(3, 3));
        for (int i = 0; i < 9; i++) {
            pnlGame[i] = new JPanel();
            pnlGame[i].setBorder(BorderFactory.createLineBorder(Color.black));
            pnlGame[i].setLayout(new GridLayout(3, 3));
            this.add(pnlGame[i]);
        }

        for (int z = 0; z < 9; z++) {
            for (int x = 0; x < 3; x++) {
                for (int y = 0; y < 3; y++) {
                    txtGame[z][x][y] = new JTextField();
                    txtGame[z][x][y].setBorder(BorderFactory
                            .createEtchedBorder());
                    txtGame[z][x][y]
                            .setFont(new Font("Dialog", Font.ITALIC, 20));// 设置字体大小
                    txtGame[z][x][y].setHorizontalAlignment(JTextField.CENTER);// 设置字体居中
                    pnlGame[z].add(txtGame[z][x][y]);
                }
            }

        }
        this.init();// 九宫格数据初始化，生成完整正确的九宫格
        this.setGameLevel(3);// 根据完整的九宫格生成不同难度的数独游戏
        for (int z = 0; z < 9; z++) {// 9宫格的行
            for (int x = 0; x < 3; x++) {// 9宫格的列
                for (int y = 0; y < 3; y++) {
                    if (grids[z][x][y].getValue() != 10) {
                        txtGame[z][x][y].setText(grids[z][x][y].getValue() + "");
                        txtGame[z][x][y].setEditable(false);
                    } else {
                        txtGame[z][x][y].getDocument().addDocumentListener(this);

                    }
                }
            }
        }
        this.setVisible(true);
    }

    /**
     * 生成完整的正确填完的九宫格
     */
    void init() {
        int value = 0;
        int[] backlocation;
        int i = 0, j = 0;
        int tag_rollback = NO, tag_validValues_exist;// 是否回滚到上一个单元格的标记,单元格的可取值是否已过滤过,可取值是否存在
        back: for (int z = 0; z < 9; z++) {// 9宫格的行
            for (int x = i; x < 3; x++) {// 9宫格的列
                for (int y = j; y < 3; y++) {
                    if (tag_rollback == 1) {
                        i = 0;
                        j = 0;
                        tag_rollback = NO;// 回滚了一次之后,回归正常状态继续遍历
                    }
                    if (null == grids[z][x][y]) {
                        grids[z][x][y] = new Grid(z, x, y);
                        tag_validValues_exist = NO;// 表示单元格的值是刚初始化的，需要过滤
                    } else {
                        tag_validValues_exist = YES;// 表示单元格的值是过滤过的
                    }
                    value = getValidValue(grids[z][x][y], tag_validValues_exist);
                    if (value == NEED_ROLLBACK) {

                        backlocation = backlocation(z, x, y);
                        reset(grids[z][x][y]);// 将该单元格重置

                        z = backlocation[0] - 1;// 这里z-2是因为跳到back之后，z会直接自增一次，所以减去2个才能跳转成上一个单元格
                        i = backlocation[1];
                        j = backlocation[2];
                        tag_rollback = YES;// 从i,j,k指定的位置开始再次遍历
                        continue back;// 重新开始前一个单元格的遍历
                    } else {
                        grids[z][x][y].setValue(value);
                    }
                }
            }
        }
    }

    // 游戏难度分级 0-3 分别为：简单、中等、困难、骨灰级
    void setGameLevel(int level) {

        int blank_max, blank_min;// blank代表每个单元格最多和最少空几个让玩家填，

        switch (level) {
            case 0:
                blank_max = 4;
                blank_min = 2;
                blankInit(blank_max, blank_min);
                break;
            case 1:
                blank_max = 6;
                blank_min = 4;
                blankInit(blank_max, blank_min);
                break;
            case 2:
                blank_max = 8;
                blank_min = 6;
                blankInit(blank_max, blank_min);
                break;
            case 3:
                blank_max = 9;
                blank_min = 6;
                blankInit(blank_max, blank_min);
                break;
        }
    }

    void blankInit(int blank_max, int blank_min) {
        Random rd = new Random();
        List<Integer> list = new ArrayList<Integer>();
        int count, index;// count代表实际的空白数,index代表空白的单元格位置

        for (int z = 0; z < 9; z++) {
            count = 0;
            while (count < blank_min) {// 每个小9宫格随机出现最大和最小空白之间的空白个数
                count = rd.nextInt(blank_max + 1);
            }
            for (int i = 1; i <= count; i++) {
                index = rd.nextInt(9);
                if (list.contains(index)) {// 如果空白选项里已经含有当前位置就重新取
                    i--;
                } else {// 如果空白选项里已经没有含有当前位置就添加
                    list.add(index);
                }
            }
            for (int j : list) {
                grids[z][j / 3][j % 3].setValue(10);// 根据j让一个小9宫格里面的9个格子随机空白
                // 10就代表这个格子空白
            }
            list.clear();
        }
    }

    /**
     * 每个单元格的类 坐标 取值 可取值队列
     *
     * @author yhd
     *
     */
    class Grid {
        private int x;// 对应每个小单元格的横坐标
        private int y;// 对应每个小单元格的纵坐标
        private int z;// 对应每个小9宫格
        private int value;// 最终取得值
        private List<Integer> validValues;// 可以取的值

        public Grid(int z, int x, int y) {
            this.x = x;
            this.y = y;
            this.z = z;
            if (validValues == null) {
                validValues = new ArrayList<Integer>();
                for (int i = 1; i < 10; i++) {
                    validValues.add(i);
                }
            }
        }

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getY() {
            return y;
        }

        public void setY(int y) {
            this.y = y;
        }

        public int getZ() {
            return z;
        }

        public void setZ(int z) {
            this.z = z;
        }

        public List<Integer> getValidValues() {
            return validValues;
        }

        public void setValidValues(List<Integer> validValues) {
            this.validValues = validValues;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }

    }

    // 0 1 2 z的布局 整体布局
    // 3 4 5
    // 6 7 8
    // 0 1
    // (0,0,0) (0,0,1) (0,0,2) (1,0,0)
    // (0,1,0) (0,1,1) (0,1,2) (1,1,0)
    // (0,2,0) (0,2,1) (0,2,2) (1,2,0)
    // 3 4
    // (3,0,0) (3,0,1) (3,0,2) (4,0,0) 详细布局

    // 当某单元格没有可取值时，回到上一个单元格
    int[] backlocation(int z, int x, int y) {
        int[] location = new int[3];// 存贮上一个单元格的位置信息 z x y
        switch (x + y) {// 根据当前单元格的坐标和找到上一个元素的坐标
            case 0:
                location[0] = z - 1;
                location[1] = 2;
                location[2] = 2;
                break;
            case 1:
                if (x < y) {
                    location[0] = z;
                    location[1] = 0;
                    location[2] = 0;
                    break;
                } else {
                    location[0] = z;
                    location[1] = 0;
                    location[2] = 2;
                    break;
                }
            case 2:
                if (x < y) {
                    location[0] = z;
                    location[1] = 0;
                    location[2] = 1;
                    break;
                } else if (x > y) {
                    location[0] = z;
                    location[1] = 1;
                    location[2] = 2;
                    break;
                } else {
                    location[0] = z;
                    location[1] = 1;
                    location[2] = 0;
                    break;
                }
            case 3:
                if (x < y) {
                    location[0] = z;
                    location[1] = 1;
                    location[2] = 1;
                    break;
                } else {
                    location[0] = z;
                    location[1] = 2;
                    location[2] = 0;
                    break;
                }
            case 4:
                location[0] = z;
                location[1] = 2;
                location[2] = 1;
                break;
        }
        return location;
    }

    /**
     * 一旦无数字可填，就回溯到上一个单元格，当前单元格的可取值列表和值重置
     *
     * @param grid
     */
    // 这里开发时遇到了java引用传递的问题，之前的代码如：grid=null
    // 结果发现只要有回溯，就会一直回溯到0 0 0，不是回溯一次改个值继续下面的遍历
    // 原来是这里的问题，引用地址传递过来后，这个grid副本存的地址清空了，并不是传进来的那个
    // grid清空了，所以导致了回溯的那些单元格一直没有被重置，无值可取，才会不断回溯的
    void reset(Grid grid) {
        int z = grid.getZ();
        int x = grid.getX();
        int y = grid.getY();
        grids[z][x][y] = null;
    }

    /**
     * 获取某个单元格可以取的值
     */
    int getValidValue(Grid grid, int tag_validValues_exist) {
        Random rd = new Random();
        int index, grid_validValues_isExist = YES, last;

        int existValue, value;

        int zx_index = 0;// 以z为标准，x轴方向的遍历起点
        int zy_index = 0;// 以z为标准，y轴方向的遍历起点

        if (tag_validValues_exist == NO) {
            switch (grid.getZ() % 3) {
                case 0:
                    zx_index = grid.getZ();
                    break; // 表示如果是0 3 6对应的小九宫格
                case 1:
                    zx_index = grid.getZ() - 1;
                    break; // 同理表示 1 4 7
                case 2:
                    zx_index = grid.getZ() - 2;
                    break; // 同理表示2 5 8
            }

            if (grid.getZ() >= 6) {
                zy_index = grid.getZ() - 6; // 对应6 7 8 y轴方向起点为0 1 2
            } else if (grid.getZ() >= 3) {
                zy_index = grid.getZ() - 3; // 对应3 4 5 y轴方向起点为0 1 2
            } else {
                zy_index = grid.getZ(); // 对应0 1 2 y轴方向起点为0 1 2
            }

            for (; zx_index < grid.getZ(); zx_index++) {
                // 去除同一行已存在的数据
                for (int y = 0; y < 3; y++) {
                    existValue = grids[zx_index][grid.getX()][y].getValue();// 获取该单元格之前同一行的不同列的值
                    grid_validValues_isExist = removeExistValue(grid,
                            existValue);
                }
            }
            if (grid_validValues_isExist == NO) {
                return NEED_ROLLBACK;
            }
            // 去除同一列已存在的数据
            for (; zy_index < grid.getZ(); zy_index += 3) {
                for (int x = 0; x < 3; x++) {
                    existValue = grids[zy_index][x][grid.getY()].getValue();// 获取该单元格之前同一列的不同列的值
                    grid_validValues_isExist = removeExistValue(grid,
                            existValue);
                }
            }
            if (grid_validValues_isExist == NO) {
                return NEED_ROLLBACK;
            }
            // 去除同一个小九宫格里面的不同的值
            for (int x = 0; x <= grid.getX(); x++) {
                if (x == grid.getX()) {// 如果已经遍历到和该单元格一行时
                    for (int y = 0; y < grid.getY(); y++) {
                        existValue = grids[grid.getZ()][x][y].getValue();
                        grid_validValues_isExist = removeExistValue(grid,
                                existValue);
                    }
                } else {// 在这单元格所在行之前行时就完全遍历列
                    for (int y = 0; y < 3; y++) {
                        existValue = grids[grid.getZ()][x][y].getValue();
                        grid_validValues_isExist = removeExistValue(grid,
                                existValue);
                    }
                }
            }
            if (grid_validValues_isExist == NO) {
                return NEED_ROLLBACK;
            }
        }
        if (grid.getValidValues().size() == 0) {
            return NEED_ROLLBACK;
        } else {
            last = grid.getValidValues().size();
        }
        if (last == 0) {
            index = 0;
        } else {
            index = rd.nextInt(last);// 返回该单元格的值
            // 因为random的nextInt范围必须是[0，last),size和index有区别，index从0开始
        }
        value = grid.getValidValues().get(index);
        removeExistValue(grid, value);// 一旦取过该值，就把这个值从该单元格中去掉
        // System.out.println(grid.getZ()+" "+grid.getX()+" "+grid.getY()+" "+value);
        return value;
    }

    /**
     * 移除单元格不能取得值和已经取过的值
     *
     * @param grid
     * @param existValue
     * @return
     */
    int removeExistValue(Grid grid, int existValue) {
        int index, grid_validValues_isExist = YES;
        if (grid.getValidValues().size() == 0) {
            grid_validValues_isExist = NO;
        }

        if (grid.getValidValues().contains(existValue))// 如果同一列已存在该数据，则去除
        {
            index = grid.getValidValues().indexOf(existValue);// 找到该值存在的index
            grid.getValidValues().remove(index);
        }
        return grid_validValues_isExist;
    }
    /**
     * 检查输入的数字是否有冲突
     * @param z1
     * @param x1
     * @param y1
     */
    private void checkValue(int z1, int x1, int y1) {
        int[] zx = new int[2];// 以z为标准，x轴方向的遍历
        int[] zy = new int[2];// 以z为标准，y轴方向的遍历
        String value = txtGame[z1][x1][y1].getText();
        value = value.substring(value.length()-1, value.length());
        switch (z1 % 3) {
            case 0:
                zx[0] = z1 + 1;
                zx[1] = z1 + 2;
                break; // 表示如果是0 3 6对应的小九宫格，检查右边2个小九宫格
            case 1:
                zx[0] = z1 - 1;
                zx[1] = z1 + 1;
                break; // 同理表示 1 4 7 检查两侧小九宫格
            case 2:
                zx[0] = z1 - 2;
                zx[1] = z1 - 1;
                ;
                break; // 同理表示2 5 8 检查左侧两个小九宫格
        }

        if (z1 >= 6) {
            zy[0] = z1 - 6; // 对应6 7 8 检查它上面2个小九宫格
            zy[1] = z1 - 3;
        } else if (z1 >= 3) {
            zy[0] = z1 - 3; // 对应3 4 5 检查它上面和下面2个小九宫格
            zy[1] = z1 + 3;
        } else {
            zy[0] = z1 + 3; // 对应0 1 2 检查它下面2个小九宫格
            zy[1] = z1 + 6;
        }

        int z;

        // 检查同一列已存在的数据
        for (int i = 0; i < 2; i++) {
            z = zy[i];
            for (int x = 0; x < 3; x++) {
                if (txtGame[z][x][y1].getText().equals(value)) {
                    txtGame[z][x][y1].setForeground(Color.red);
                    txtGame[z1][x1][y1].setForeground(Color.red);
                    //将冲突的两个文本框的值交替放入map
                    warnFiledMap.put(txtGame[z][x][y1],txtGame[z1][x1][y1]);
                    warnFiledMap.put(txtGame[z1][x1][y1],txtGame[z][x][y1]);
                    return;
                }
            }
        }

        // 检查同一行已存在的数据
        for (int i = 0; i < 2; i++) {
            z = zx[i];
            for (int y = 0; y < 3; y++) {
                if (txtGame[z][x1][y].getText().equals(value)) {
                    txtGame[z][x1][y].setForeground(Color.red);
                    txtGame[z1][x1][y1].setForeground(Color.red);
                    //将冲突的两个文本框的值交替放入map
                    warnFiledMap.put(txtGame[z][x1][y],txtGame[z1][x1][y1]);
                    warnFiledMap.put(txtGame[z1][x1][y1],txtGame[z][x1][y]);
                    return;
                }
            }
        }

        // 检查同一个小九宫格里面的不同的值
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                if (!(x == x1 && y == y1)
                        && txtGame[z1][x][y].getText().equals(value)) {

                    txtGame[z1][x][y].setForeground(Color.red);
                    txtGame[z1][x1][y1].setForeground(Color.red);
                    //将冲突的两个文本框的值交替放入map
                    warnFiledMap.put(txtGame[z1][x][y],txtGame[z1][x1][y1]);
                    warnFiledMap.put(txtGame[z1][x1][y1],txtGame[z1][x][y]);
                    return;
                }
            }
        }

    }

    // 文本属性的变化
    @Override
    public void changedUpdate(DocumentEvent documentEvent) {
    }
    //插入了新数据
    @Override
    public void insertUpdate(DocumentEvent documentEvent) {
        int[] location = new int[3];
        int x,y,z;
        String value;
        doc = (Document) documentEvent.getDocument();
        // 控制文本框显示的数字始终1个并且是刚键入的数字,没有冲突
        this.validateInput(NO);
        //如果是数字继续下面的操作
        if(is_num == YES){
            //当存在冲突的时候接收不能冲突field以外的输入
            if (warnFiledMap.size() != 0) {
                // 根据触发的docment找到填充这个docment的field坐标
                this.findFeildLocationByDoc(location);
                z = location[0];
                x = location[1];
                y = location[2];
                //如果修改的是冲突域的值
                if(warnFiledMap.get(txtGame[z][x][y])!=null){
                    value = txtGame[z][x][y].getText();
                    value = value.substring(value.length()-1, value.length());
                    if(!value.equals(warnFiledMap.get(txtGame[z][x][y]).getText())){
                        txtGame[z][x][y].setForeground(Color.black);
                        warnFiledMap.get(txtGame[z][x][y]).setForeground(Color.black);
                        warnFiledMap.clear();//清空冲突域
                    }
                    // 根据坐标去检查填入数字是否正确
                    this.checkValue(location[0], location[1], location[2]);
                }
                //禁止其它文本域输入
                else{
                    // 有冲突存在，无法输入
                    this.validateInput(YES);
                }
            }
            else{
                this.findFeildLocationByDoc(location);
                // 根据坐标去检查填入数字是否正确
                this.checkValue(location[0], location[1], location[2]);
            }
        }
        else{
            is_num = YES;//将数字开关重置，等待下次重新输入
        }
    }

    /**
     * 控制文本框只能输入数字，并且是1个    并且显示当前刚输入的数字
     */
    void validateInput(int conflict) {
        String value;
        // 下面都是开启线程来重置输入的值 否则insertUpdate的写锁会导致无法控制输入

        //如果不是数字就禁止输入
        try {
            value = doc.getText(0, doc.getLength());
            value = value.substring(value.length()-1, value.length());
            if(!Pattern.matches("\\d*", value)){
                new Thread(new Thread() {
                    public void run() {
                        try {
                            doc.remove(doc.getLength()-1, 1);// 这里会触发一个removeUpdate事件 将刚输入的非法字符清空
                        } catch (Exception exp) {
                            System.out.println("Error: " + exp.toString());
                        }
                    }
                }).start();
                is_num = NO;//输入的不是数字
                is_remove_by_insert = YES;//remove由insert导致
                return ;//不是数字就直接返回
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }

        if(conflict==YES && doc.getLength() > 0){
            try {
                new Thread(new Thread() {
                    public void run() {
                        try {
                            doc.remove(0, doc.getLength());// 这里会触发一个removeUpdate事件,输入全部清空
                        } catch (Exception exp) {
                            System.out.println("Error: " + exp.toString());
                        }
                    }
                }).start();
                is_remove_by_insert = YES;//remove由insert导致
            } catch (Exception ex) {
                System.out.println("Error: " + ex.toString());
            }
        }

        if (doc.getLength() > 1 && conflict==NO) {
            try {
                new Thread(new Thread() {
                    public void run() {
                        try {
                            doc.remove(0, doc.getLength() - 1);// 这里会触发一个removeUpdate事件
                        } catch (Exception exp) {
                            System.out.println("Error: " + exp.toString());
                        }
                    }
                }).start();
                is_remove_by_insert = YES;//remove由insert导致
            } catch (Exception ex) {
                System.out.println("Error: " + ex.toString());
            }
        }

        int z;
        //检验所有是否都填完并且无冲突
        if(conflict==NO){
            for(z=0;z<9;z++){
                for(int x=0;x<3;x++){
                    for(int y=0;y<3;y++){
                        if(txtGame[z][x][y].getText().equals("")){
                            return;
                        }
                    }
                }
            }
            //如果所有的都填完就代表通关
            if(z==9){
                System.out.println("恭喜通关！");
            }
        }
    }

    /**
     * 根据doc找到doc所在的field
     * @param location
     */
    void findFeildLocationByDoc(int[] location) {
        for (int z = 0; z < 9; z++) {
            for (int x = 0; x < 3; x++) {
                for (int y = 0; y < 3; y++) {
                    if (txtGame[z][x][y].getDocument() == doc) {
                        location[0] = z;
                        location[1] = x;
                        location[2] = y;
                    }
                }
            }
        }
    }
    /**
     * 移除数字的时候更新
     */
    @Override
    public void removeUpdate(DocumentEvent documentEvent) {
        doc = (Document) documentEvent.getDocument();
        int[] location = new int[3];
        int x,y,z;

        if(is_remove_by_insert == NO){
            this.findFeildLocationByDoc(location);
            z = location[0];
            x = location[1];
            y = location[2];
            if(warnFiledMap.get(txtGame[z][x][y])!=null){
                txtGame[z][x][y].setForeground(Color.black);
                warnFiledMap.get(txtGame[z][x][y]).setForeground(Color.black);
            }
        }
    }

    public static void main(String[] args) {
        new SudokuGame();
    }
}
