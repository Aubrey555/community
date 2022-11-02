package com.nowcoder.community.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

//前缀树及前缀树过滤方法都定义在工具类中,通过@Component注解交由Spring容器进行管理,从而在任何地方都可使用。
@Component  //通用的敏感词过滤器,交给Spring进行管理
@Slf4j
public class SensitiveFilter {


    // 替换符:敏感词替换字符
    private static final String REPLACEMENT = "***";

    // 根节点:前缀树的根节点,一般为空即可
    private TrieNode rootNode = new TrieNode();
    //前缀树的初始化方法
    @PostConstruct    //作用:在容器实例化此bean对象(SensitiveFilter),即调用构造器(服务器启动时)之后,调用此方法
    public void init() {
        try (
                //将流写入try中,会在catch后自动关闭
                //1.加载敏感词文件的字节流对象
                InputStream is = this.getClass().getClassLoader().getResourceAsStream("sensitive-words.txt");
                //2.将敏感词字节流对象转换为字符流,读取更快
                InputStreamReader isr = new InputStreamReader(is);
                //3.通过字符缓冲流对敏感词文件进行读写
                BufferedReader reader = new BufferedReader(isr);
        ) {
            String keyword;//表示字符缓冲流读取到的每个字符对象
            //4.字符缓冲流读取到每个字符,赋值给keyword,只要当前keyword不为空,表示没有读取结束
            while ((keyword = reader.readLine()) != null) {//每个敏感词通过换行分隔
                // 5.调用方法,将当前字符,添加到前缀树
                this.addKeyword(keyword);
            }
        } catch (IOException e) {
            log.error("加载敏感词文件失败: " + e.getMessage());
        }
    }

    /**
     * 将一个敏感词添加到前缀树中的方法
     *      1.创建临时节点,指向头节点,遍历传入的敏感词字符串
     *      2.遍历得到当前字符c,得到当前节点tempNode以c为字符对应的子节点subNode,
     *          2.1 如果subNode为空,表示当前节点的子节点没有字符c,则加入该字符对应的子节点
     *          2.2 更新节点tempNode为字符c对应的子节点subNode(不管subNode是否为空,都需要执行)
     *          2.3 如果遍历到了当前敏感词字符串的最后的一个字符,则为该结点设置结束标识。
     * @param keyword
     */
    private void addKeyword(String keyword) {
        TrieNode tempNode = rootNode;
        for (int i = 0; i < keyword.length(); i++) {
            char c = keyword.charAt(i);
            TrieNode subNode = tempNode.getSubNode(c);
            //2.1 如果subNode为空,表示当前节点的子节点没有字符c,则加入该字符对应的子节点
            if (subNode == null) {
                // 初始化子节点
                subNode = new TrieNode();
                tempNode.addSubNode(c, subNode);
            }
            //2.2 更新节点tempNode为字符c对应的子节点subNode(不管subNode是否为空,都需要执行)
            tempNode = subNode;
            //2.3 如果遍历到了当前敏感词字符串的最后的一个字符,则为该结点设置结束标识。
            if (i == keyword.length() - 1) {
                tempNode.setKeywordEnd(true);
            }
        }
    }

    /**
     * 过滤敏感词(公有方法,调用此方法过滤敏感词)
     *      思路:1.初始化三个指针变量,指针1 tempNode初始指向头节点,用于遍历前缀树;慢快指针2/3用于遍历给定文本;使用StringBuilder保存过滤后的文本
     *          2. 只要快指针3小于给定文本的大小,则一直进行敏感词判断(快指针3即作为敏感词结束条件,可能会比指针2优先到达结尾)
     *              2.1 得到当前文本的字符c,如果c为普通符号,直接跳过(调用方法),慢快指针2/3都需要+1
     *              2.2 通过指针1得到当前节点的下级节点tempNode(以字符c作为条件),如果得到的节点tempNode为空
     *                  2.2.1 此时表示字符c不是敏感词,直接将begin指向位置的字符加入sb中;则慢指针begin+1,快指针指向当前慢指针位置,继续判断(指针1归为)
     *              2.3 如果前缀树指针1指向节点tempNode为叶子节点(表示敏感词判断结束)、
     *                  2.3.1 此时sb之后拼接替换符,并且快指针3++,慢指针2指向快指针3
     *              2.4 否则,此时前缀树节点tempNode不为空,也不是叶子节点
     *                  则继续判断文本串中的下个字符:(慢指针2不动,快指针3只要未越界,则递增,判断下个敏感词字符)
     *          3. 将最后一批字符计入结果(遍历最终慢指针指向字符到快指针指向的text最后字符之间,都是合法字符,直接加入)
     *          4.返回sb
     * @param text 待过滤的文本(可能含有敏感词)
     * @return 过滤后的文本
     */
    public String filter(String text) {
        if (StringUtils.isBlank(text)) {
            return null;
        }
        TrieNode tempNode = rootNode;// 指针1
        int begin = 0; // 指针2
        int position = 0;//指针3(作为过滤文本结束标识)
        StringBuilder sb = new StringBuilder(); // 结果

        while (position < text.length()) {
            char c = text.charAt(position);

            //2.1 跳过符号:得到当前文本的字符c,如果c为普通符号,直接跳过(调用方法)
            if (isSymbol(c)) {
                // 若指针1处于根节点(此时指针2指向的字符还不是疑似敏感词),将此符号计入结果,让指针2向下走一步
                if (tempNode == rootNode) {
                    sb.append(c);
                    begin++;
                }
                //此时c为符号,使用的是指针3指向,因此无论符号在开头或中间,指针3都向下走一步
                position++;
                continue;
            }

            // 2.2 通过指针1得到当前节点的下级节点tempNode(以字符c作为条件),如果得到的节点tempNode为空
            tempNode = tempNode.getSubNode(c);
            if (tempNode == null) {
                // 以begin开头的字符串不是敏感词
                sb.append(text.charAt(begin));
                // 进入下一个位置
                begin++;
                position = begin;
                // 重新指向根节点
                tempNode = rootNode;
            } else if (tempNode.isKeywordEnd()) {// 2.3 如果前缀树指针1指向节点tempNode为叶子节点(表示敏感词判断结束)、
                // 发现敏感词,将begin~position字符串替换掉
                sb.append(REPLACEMENT);
                // 进入下一个位置(此时 慢指针->快指针中间的字符即为敏感词字符)
                position++;//快指针++
                begin = position;//慢指针指向快指针
                // 重新指向根节点
                tempNode = rootNode;
            } else {// 2.4 否则,此时前缀树节点tempNode不为空,也不是叶子节点
                //则继续判断文本串中的下个字符:(慢指针2不动,快指针3只要未越界,则递增,判断下个敏感词字符)
                if(position < text.length()-1){
                    // 检查下一个字符（快指针3未越界）
                    position++;
                }
            }
        }
        // 3.将最后一批字符计入结果(遍历最终慢指针指向字符到快指针指向的text最后字符之间,都是合法字符,直接加入)
        sb.append(text.substring(begin));
        return sb.toString();
    }

    // 判断传入字符是否为符号
    private boolean isSymbol(Character c) {
        // 0x2E80~0x9FFF 是东亚文字范围,因此需要在此范围之外
        return !CharUtils.isAsciiAlphanumeric(c) && (c < 0x2E80 || c > 0x9FFF);
    }


    //前缀树的某个节点(该前缀树结构,只在此敏感词过滤器中使用,因此定义为私有的内部类)
    private class TrieNode {
        //关键词结束标识
        private boolean isKeywordEnd = false;
        // 当前节点的子节点(key是下级字符(即子节点字符),value是对应的下级子节点,使用map进行封装)
        private Map<Character, TrieNode> subNodes = new HashMap<>();
        //表示某节点是否为敏感词结束字符
        public boolean isKeywordEnd() {
            return isKeywordEnd;
        }
        //设置该节点为敏感词结束字符
        public void setKeywordEnd(boolean keywordEnd) {
            isKeywordEnd = keywordEnd;
        }
        // 添加子节点
        public void addSubNode(Character c, TrieNode node) {
            subNodes.put(c, node);
        }
        // 获取子节点(通过key获取value)
        public TrieNode getSubNode(Character c) {
            return subNodes.get(c);
        }

    }

}
