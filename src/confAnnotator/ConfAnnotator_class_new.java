package confAnnotator;

import edu.illinois.cs.cogcomp.chunker.main.lbjava.Chunker;
import edu.illinois.cs.cogcomp.lbjava.nlp.SentenceSplitter;
import edu.illinois.cs.cogcomp.lbjava.nlp.Word;
import edu.illinois.cs.cogcomp.lbjava.nlp.WordSplitter;
import edu.illinois.cs.cogcomp.lbjava.nlp.seg.PlainToTokenParser;
import edu.illinois.cs.cogcomp.lbjava.parse.Parser;
import edu.stanford.nlp.coref.CorefCoreAnnotations;
import edu.stanford.nlp.coref.data.CorefChain;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ConfAnnotator_class_new {
    public void setStr(String str) {
        this.str = str;
    }

    public void setN(String n) {
        this.n = n;
    }

    private String str;
    private String n;
    private  int id;

    public void setClass_n(String class_n) {
        this.class_n = class_n;
    }

    private  String class_n;
    Properties props = new Properties();

    StanfordCoreNLP pipeline;

    public void setId(int id) {
        this.id = id;
    }

    public static  void  main(String[] args){
        ConfAnnotator_class_new confAnnotator = new ConfAnnotator_class_new();
        confAnnotator.replaceWord();
    }

    //--------replace some wrong words
    public  void replaceWord(){
        String driver = "com.mysql.jdbc.Driver";
        String url = "jdbc:mysql://10.131.252.156/fdroid";
        String user = "root";
        String password = "root";

        try {
            Class.forName(driver);
            Connection conn = DriverManager.getConnection(url, user, password);

            Statement statement = conn.createStatement();
            String sql = "select class_id,name,class_name,conf_description from jdk_class ";
            ResultSet rs = statement.executeQuery(sql);

            while(rs.next()) {
                setId(rs.getInt("class_id"));
                setN(rs.getString("name"));
                setClass_n(rs.getString("class_name"));
                if(rs.getString("conf_description")==null){
                    continue;
                }
                setStr(rs.getString("conf_description").trim());

                if(str.equals("")||str==null){
                    continue;
                }
                System.out.println(id);

                Boolean in = false;

                // name (an a the this)+classname iswhen iswhat iswhere iswhile isif
                if(str.contains(n+" is "+n)){
                    setStr(str.replace(n+" is "+n,n));
                    in = true;
                }else if(str.contains(n+" is A "+class_n)){
                    setStr(str.replace(n+" is A "+class_n,n));
                    in = true;
                }else if(str.contains(n+" is An "+class_n)){
                    setStr(str.replace(n+" is An "+class_n,n));
                    in = true;
                }else if(str.contains(n+" is The "+class_n)){
                    setStr(str.replace(n+" is The "+class_n,n));
                    in = true;
                }else if(str.contains(n+" is This "+class_n)){
                    setStr(str.replace(n+" is This "+class_n,n));
                    in = true;
                }else if(str.contains(n+" is When")){
                    setStr(str.replace(n+" is When","When"));
                    in = true;
                }else if(str.contains(n+" is What")){
                    setStr(str.replace(n+" is What","What"));
                    in = true;
                }else if(str.contains(n+" is Where")){
                    setStr(str.replace(n+" is Where","Where"));
                    in = true;
                }else if(str.contains(n+" is While")){
                    setStr(str.replace(n+" is While","While"));
                    in = true;
                }else if(str.contains(n+" is If")){
                    setStr(str.replace(n+" is If","If"));
                    in = true;
                }

                if(in){
                    Statement statement1 = conn.createStatement();
                    String sql1 = "update jdk_class set conf_description = \""+str+"\" where class_id = "+id;
                    statement1.execute(sql1);
                    statement1.close();
                }

            }

            rs.close();
            conn.close();
        } catch(ClassNotFoundException e) {
            System.out.println("Sorry,can`t find the Driver!");
            e.printStackTrace();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    //-------------change the conf to none
    public void changeConf(){

        Annotation document = new Annotation(str);

        pipeline.annotate(document);

        String origin = "";
        for (CorefChain cc : document.get(CorefCoreAnnotations.CorefChainAnnotation.class).values()) {
            origin = cc.getMentionsInTextualOrder().get(0).mentionSpan;

            for(int i=1;i<cc.getMentionsInTextualOrder().size();i++){
                String replace = cc.getMentionsInTextualOrder().get(i).mentionSpan;
                str = str.replaceFirst(replace,origin);
            }
            //System.out.println(str);
        }
        /*for (CoreMap sentence : document.get(CoreAnnotations.SentencesAnnotation.class)) {
            System.out.println("---");
            System.out.println("mentions");
            for (Mention m : sentence.get(CorefCoreAnnotations.CorefMentionsAnnotation.class)) {
                System.out.println("\t" + m);
            }
        }*/
    }


    //----------------deprecated
    public List findDeprecate(){
        List<String> list = new ArrayList<String>();
        if(str.contains("was deprecated in API level")){
            list.add(n);
            list.add("deprecated");
        }
        return list;
    }

    public String changeToClass(){
        str = str.replace("This Class",n);
        str = str.replace("this Class",n);
        str = str.replace("this class",n);
        str = str.replace("This class",n);
        str = str.replace("This Interface",n);
        str = str.replace("This interface",n);
        str = str.replace("this Interface",n);
        str = str.replace("this interface",n);
        return str;
    }

    //-----------get first world type   0 verb    1 none
    public int getFirst(){
        setStr("Returns ");
        int index=str.indexOf(" ");
        String getStr;
        if(index != -1){
            getStr=str.substring(0,index).toLowerCase();
        }else{
            getStr = str.toLowerCase();
        }


        FileWriter fw = null;
        try {
            fw = new FileWriter("in1.txt");
            fw.write(getStr);
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Chunker chunker = new Chunker();
        Parser parser = new PlainToTokenParser(new WordSplitter(new SentenceSplitter("in1.txt")));
        Word w = (Word) parser.next();
        String prediction = chunker.discreteValue(w);
        //System.out.println(prediction);

        if("B-VP".equals(prediction)){
            return 0;//-----------verb
        }else {
            return 1;//----------none
        }
    }

    //-------read data from db
    public void getDataFromDB(){
        String driver = "com.mysql.jdbc.Driver";
        String url = "jdbc:mysql://10.131.252.156/fdroid";
        String user = "root";
        String password = "root";
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner,parse,mention,coref");
        pipeline = new StanfordCoreNLP(props);

        try {
            Class.forName(driver);
            Connection conn = DriverManager.getConnection(url, user, password);

            Statement statement = conn.createStatement();
            String sql = "select class_id,name,conf_description from jdk_class ";
            ResultSet rs = statement.executeQuery(sql);

            while(rs.next()) {
                setId(rs.getInt("class_id"));
                setN(rs.getString("name"));
                if(rs.getString("conf_description")==null){
                    continue;
                }
                setStr(rs.getString("conf_description").trim());

                if(str.equals("")||str==null){
                    continue;
                }
                System.out.println(id);

                //----------change the class name
                setStr(changeToClass());


                Statement statement1 = conn.createStatement();
                String sql1 = "update jdk_class set conf_description = \""+str+"\" where class_id = "+id;
                statement1.execute(sql1);
                statement1.close();
            }

            rs.close();
            conn.close();
        } catch(ClassNotFoundException e) {
            System.out.println("Sorry,can`t find the Driver!");
            e.printStackTrace();
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}