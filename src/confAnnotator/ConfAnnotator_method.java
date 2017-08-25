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

public class ConfAnnotator_method extends Thread{
    public void setStr(String str) {
        this.str = str;
    }

    public void setN(String n) {
        this.n = n;
    }

    private String str;
    private String n;
    private  int id;
    Properties props = new Properties();

    public void setStart(int start) {
        this.start = start;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    private int start = -1;
    private int end = -1;

    public void setFilename(String filename) {
        this.filename = filename;
    }

    private String filename = "";

    StanfordCoreNLP pipeline;

    public void setId(int id) {
        this.id = id;
    }

    public static  void  main(String[] args){
        Thread confAnnotator1 = new ConfAnnotator_method(2179,9999,"in.txt");
        Thread confAnnotator2 = new ConfAnnotator_method(10510,19999,"in1.txt");
        /*Thread confAnnotator3 = new ConfAnnotator_method(20000,29999,"in2.txt");
        Thread confAnnotator4 = new ConfAnnotator_method(30000,39999,"in3.txt");
        Thread confAnnotator5 = new ConfAnnotator_method(40000,49999,"in4.txt");
        Thread confAnnotator6 = new ConfAnnotator_method(50000,59999,"in5.txt");
        Thread confAnnotator7 = new ConfAnnotator_method(60000,69999,"in6.txt");
        Thread confAnnotator8 = new ConfAnnotator_method(70000,79999,"in7.txt");
        Thread confAnnotator9 = new ConfAnnotator_method(80000,89999,"in8.txt");*/

        confAnnotator1.start();
        confAnnotator2.start();
         /*confAnnotator3.start();
        confAnnotator4.start();
        confAnnotator5.start();
        confAnnotator6.start();
        confAnnotator7.start();
        confAnnotator8.start();
        confAnnotator9.start();*/



    }

    public ConfAnnotator_method(int start, int end, String filename){
        setStart(start);
        setEnd(end);
        setFilename(filename);
    }
    public void run(){
        getDataFromDB();
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

    public String changeToMethod(){
        str = str.replace("This method",n);
        str = str.replace("this Method",n);
        str = str.replace("this method",n);
        str = str.replace("This Method",n);
        return str;
    }

    //-----------get first world type   0 verb    1 none
    public int getFirst(){

        int index=str.indexOf(" ");
        String getStr;
        if(index != -1){
            getStr=str.substring(0,index).toLowerCase();
        }else{
            getStr = str.toLowerCase();
        }

        if("if".equals(getStr)||"when".equals(getStr)||"while".equals(getStr)){
            return 2;
        }else if("is".equals(getStr)){
            return 0;//-----------verb
        }

        FileWriter fw = null;
        try {
            fw = new FileWriter(filename);
            fw.write(getStr);
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Chunker chunker = new Chunker();
        Parser parser = new PlainToTokenParser(new WordSplitter(new SentenceSplitter(filename)));
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
            String sql = "select method_id,name,handled_description from jdk_method ";
            ResultSet rs = statement.executeQuery(sql);

            while(rs.next()) {
                setId(rs.getInt("method_id"));
                setN(rs.getString("name"));
                setStr(rs.getString("handled_description"));
                if(id>end|| id<start){
                    continue;
                }
                if(str == null||str.equals("")){
                    continue;
                }else {
                    setStr(str.trim());
                }
                System.out.println(id);
                //---------add none at first place
                if(getFirst()==0){
                    setStr(n+" "+str);
                }else if(getFirst()==2){
                }else{
                    setStr(n+" is "+str);
                }

                //----------change the class name
                setStr(changeToMethod());

                try {
                    changeConf();
                }catch (Exception e){
                    e.printStackTrace();
                }

                Statement statement1 = conn.createStatement();
                String sql1 = "update jdk_method set conf_description = \""+str+"\" where method_id = "+id;
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