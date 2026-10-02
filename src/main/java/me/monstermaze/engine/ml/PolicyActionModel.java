package me.monstermaze.engine.ml;

import me.monstermaze.engine.api.Action;
import me.monstermaze.engine.api.GameState;
import me.monstermaze.engine.maze.MazeGraph;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dependency-free reader/inference for the same policy-model.json format used
 * by MonsterMazeAI (52 inputs, 48/24 hidden layers).
 */
public final class PolicyActionModel {
    private static final int INPUTS = 52;
    private static final int HIDDEN_1 = 48;
    private static final int HIDDEN_2 = 24;

    private final double[] inputMean;
    private final double[] inputStd;
    private final double targetMean;
    private final double targetStd;
    private final double targetMin;
    private final double targetMax;
    private final double[][] w1;
    private final double[] b1;
    private final double[][] w2;
    private final double[] b2;
    private final double[] w3;
    private final double b3;

    private PolicyActionModel(double[] inputMean, double[] inputStd,
                              double targetMean, double targetStd,
                              double targetMin, double targetMax,
                              double[][] w1, double[] b1,
                              double[][] w2, double[] b2,
                              double[] w3, double b3) {
        this.inputMean=inputMean; this.inputStd=inputStd;
        this.targetMean=targetMean; this.targetStd=targetStd;
        this.targetMin=targetMin; this.targetMax=targetMax;
        this.w1=w1; this.b1=b1; this.w2=w2; this.b2=b2; this.w3=w3; this.b3=b3;
    }

    public static PolicyActionModel load(Path path) throws IOException {
        if (path == null) throw new IllegalArgumentException("path");
        String json = Files.readString(path, StandardCharsets.UTF_8);
        double[] mean = array(json,"input_mean",INPUTS);
        double[] std = array(json,"input_std",INPUTS);
        for (double v: std) if (!Double.isFinite(v) || v <= 1e-12) throw new IllegalArgumentException("Invalid input std");
        double targetMean = scalar(json,"target_mean");
        double targetStd = scalar(json,"target_std");
        double targetMin = scalar(json,"target_min");
        double targetMax = scalar(json,"target_max");
        if (!Double.isFinite(targetStd) || targetStd <= 1e-12) throw new IllegalArgumentException("Invalid target std");
        if (!Double.isFinite(targetMin) || !Double.isFinite(targetMax) || targetMin > targetMax)
            throw new IllegalArgumentException("Invalid target bounds");
        return new PolicyActionModel(mean,std,targetMean,targetStd,targetMin,targetMax,
                matrix(json,"w1",INPUTS,HIDDEN_1), array(json,"b1",HIDDEN_1),
                matrix(json,"w2",HIDDEN_1,HIDDEN_2), array(json,"b2",HIDDEN_2),
                array(json,"w3",HIDDEN_2), array(json,"b3",1)[0]);
    }

    public double predict(GameState state, Action action, MazeGraph graph) {
        return predict(PolicyLearningFeatures.extract(state, action, graph));
    }

    public double predict(double[] features) {
        if (features == null || features.length != INPUTS) throw new IllegalArgumentException("Expected 52 policy features");
        double[] a1 = new double[HIDDEN_1];
        for (int j=0;j<HIDDEN_1;j++) {
            double sum=b1[j];
            for(int i=0;i<INPUTS;i++) sum += ((features[i]-inputMean[i])/inputStd[i])*w1[i][j];
            a1[j]=Math.max(0.0,sum);
        }
        double[] a2 = new double[HIDDEN_2];
        for(int j=0;j<HIDDEN_2;j++) {
            double sum=b2[j];
            for(int i=0;i<HIDDEN_1;i++) sum += a1[i]*w2[i][j];
            a2[j]=Math.max(0.0,sum);
        }
        double out=b3;
        for(int i=0;i<HIDDEN_2;i++) out += a2[i]*w3[i];
        double predicted=out*targetStd+targetMean;
        return Math.max(targetMin,Math.min(targetMax,predicted));
    }

    private static double scalar(String json,String key){
        Matcher m=Pattern.compile("""+Pattern.quote(key)+""\\s*:\\s*([-+0-9.eE]+)").matcher(json);
        if(!m.find()) throw new IllegalArgumentException("Missing scalar: "+key);
        return Double.parseDouble(m.group(1));
    }
    private static double[] array(String json,String key,int expected){
        String body=bracketBody(json,key);
        Matcher m=Pattern.compile("[-+]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)(?:[eE][-+]?\\d+)?").matcher(body);
        double[] out=new double[expected]; int count=0;
        while(m.find()){ if(count>=expected) throw new IllegalArgumentException("Too many values: "+key); out[count++]=Double.parseDouble(m.group());}
        if(count!=expected) throw new IllegalArgumentException("Expected "+expected+" values in "+key+", got "+count);
        return out;
    }
    private static double[][] matrix(String json,String key,int rows,int cols){
        String body=bracketBody(json,key); double[][] out=new double[rows][cols];
        int depth=0,row=0,start=-1;
        for(int i=0;i<body.length();i++){
            char ch=body.charAt(i);
            if(ch=='['){ if(depth==0) start=i+1; depth++; }
            else if(ch==']'){ depth--; if(depth==0){
                if(row>=rows) throw new IllegalArgumentException("Too many rows: "+key);
                String[] values=body.substring(start,i).split(",");
                if(values.length!=cols) throw new IllegalArgumentException("Wrong columns in "+key+" row "+row);
                for(int c=0;c<cols;c++) out[row][c]=Double.parseDouble(values[c].trim());
                row++;
            }}
        }
        if(row!=rows) throw new IllegalArgumentException("Expected "+rows+" rows in "+key+", got "+row);
        return out;
    }
    private static String bracketBody(String json,String key){
        int marker=json.indexOf("""+key+""");
        if(marker<0) throw new IllegalArgumentException("Missing field: "+key);
        int start=json.indexOf('[',marker);
        if(start<0) throw new IllegalArgumentException("Missing array: "+key);
        int depth=0;
        for(int i=start;i<json.length();i++){
            char ch=json.charAt(i);
            if(ch=='[') depth++;
            else if(ch==']'){ depth--; if(depth==0) return json.substring(start+1,i); }
        }
        throw new IllegalArgumentException("Unterminated array: "+key);
    }
}
