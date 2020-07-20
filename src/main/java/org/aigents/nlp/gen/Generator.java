/*
 * MIT License
 * 
 * Copyright (c) 2020-Present by Vignav Ramesh and Anton Kolonin, Aigents®
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package main.java.org.aigents.nlp.gen;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import main.java.org.aigents.nlp.lg.Dictionary;
import main.java.org.aigents.nlp.lg.Disjunct;
import main.java.org.aigents.nlp.lg.Loader;
import main.java.org.aigents.nlp.lg.Rule;
import main.java.org.aigents.nlp.lg.Word;

public class Generator {
  public static Dictionary dict, hyphenated;
  
  public static void main(String[] args) throws IOException {
    if (args.length == 2) {
      int single = 0;
      int multOne = 0;
      int multNo = 0;
      int no = 0;
      try {
        if (args[0].contains("/4.0.dict")) {
          Dictionary[] dicts = Loader.buildLGDict(args[0]);
          dict = dicts[0];
          hyphenated = dicts[1];
        } else {
          dict = Loader.grammarBuildLinks(args[0], false);
        }
        List<String> list = getList(args[1]);
        List<String[]> words = processSentences(args[1]);
        if (words == null) {
          System.err.println("Error loading and tokenizing sentences.");
          return;
        }
        int idx = -1;
        for (String[] w : words) {
          idx++;
          HashSet<String> sentence = generateSentence(w);
          System.out.println(Arrays.toString(w) + ": " + sentence);
          String s = list.get(idx);
          if (sentence.size() > 1) {
            boolean one = false;
            for (String sen2 : sentence) {
              if (sen2.equals(s)) {
                one = true;
                continue;
              }
              String sen = sen2.substring(0, sen2.length() - 1);
              String[] senParts = sen.split(" ");
              String[] sParts = s.substring(0, s.length() - 1).split(" ");
              ArrayList<String> mismatches = new ArrayList<>();
              for (int i = 0; i < sParts.length; i++) {
                if (!senParts[i].equals(sParts[i])) {
                  mismatches.add(senParts[i]);
                }
              }
              System.out.println("      The words " + mismatches + " are in the wrong place.");
              System.out.println("      While the sentence \"" + sen2
                  + "\" is grammatically valid, it is contextually wrong.");
            }
            if (!one) multNo++;
            else multOne++;
          } else if (sentence.size() == 1) {
            single++;
          } else {
            no++;
          }
        }
        System.out.println("Single correct: " + single + "/" + words.size());
        System.out.println("Multiple with one correct: " + multOne + "/" + words.size());
        System.out.println("Multiple with none correct: " + multNo + "/" + words.size());
        System.out.println("None correct: " + no + "/" + words.size());
        System.out.println("Accuracy: " + ((double) single) / words.size());
      } catch (Exception e) {
        System.err.println("Error building dictionary. Please try again with a different filename.");
      }
    } else if (args.length > 2) {
      try {
        if (args[0].contains("/4.0.dict")) {
          Dictionary[] dicts = Loader.buildLGDict(args[0]);
          dict = dicts[0];
          hyphenated = dicts[1];
        } else {
          dict = Loader.grammarBuildLinks(args[0], false);
        }
        String[] words = new String[args.length - 1];
        for (int i = 1; i < args.length; i++) {
          words[i - 1] = args[i];
        }
        System.out.println(Arrays.toString(words) + ": " + generateSentence(words));
      } catch (Exception e) {
        System.err.println("Error building dictionary. Please try again with a different filename.");
      }
    } else {
      System.out.println("No command line parameters given.");
    }
    Dictionary[] dicts = Loader.buildLGDict("en/4.0.dict");
    dict = dicts[0];
    hyphenated = dicts[1];
    for (String[] words : processSentences("poc_english.txt")) {
      System.out.println(generateSentence(words));
    }
  }
  
  private static boolean connects(String left, String right) {
    if (!checkLR(left, right)) return false;
    ArrayList<Rule> leftList = dict.getRule(left), rightList = dict.getRule(right);
    if (leftList.size()==0) {
      System.err.println("Word '" + left + "' not found in dictionary.");
      System.exit(0);
    }
    if (rightList.size()==0) {
      System.err.println("Word '" + right + "' not found in dictionary.");
      System.exit(0);
    }
    for (Rule leftRule : leftList) {
      for (Rule rightRule : rightList) {
        String lr = leftRule.toString();
        String rr = rightRule.toString();
        lr=beforeNull(lr);
        rr=beforeNull(rr);
        lr = replaceNull(lr);
        rr = replaceNull(rr);
        
        ArrayList<String> Lops = new ArrayList<>(), Rops = new ArrayList<>(), 
            Lcosts = new ArrayList<>(), Rcosts = new ArrayList<>();
        while (lr.contains("{")) {
          int start = lr.indexOf("{");
          int end = 0;
          int numC = 1, num = 0;
          for (int i =start+1;i < lr.length(); i++) {
            if (lr.charAt(i) == '{') numC++;
            else if (lr.charAt(i) == '}') num++;
            if (numC==num) {
              end=i+1; break;
            }
          }
          Lops.add(lr.substring(start,end));
          lr = lr.substring(0, start)+lr.substring(end);
        }
        lr = fixString(lr);
        
        while (rr.contains("{")) {
          int start = rr.indexOf("{");
          int end = 0;
          int numC = 1, num = 0;
          for (int i =start+1;i < rr.length(); i++) {
            if (rr.charAt(i) == '{') numC++;
            else if (rr.charAt(i) == '}') num++;
            if (numC==num) {
              end=i+1; break;
            }
          }
          Rops.add(rr.substring(start,end));
          rr = rr.substring(0, start)+rr.substring(end);
        }
        rr = fixString(rr);

        for (String l : lr.split(" or ")) {
          int ri = -1;
          rloop : for (String r : rr.split(" or ")) {
            ri++;
            int numC = 0, num = 0;
            for (int q = ri; q < rr.split(" or ").length; q++) {
              boolean a = false;
              for (char c : rr.split(" or ")[q].toCharArray()) {
                if (c == '(') numC++;
                else if (c == ')') num++;
                else if (c == '&') a = true;
              }
              if (a) continue rloop;
              if (num > numC) break;
            }
            l=format(l); r=format(r);
            String fl = ""; 
            for (String p : l.split(" & ")) {
              if (!p.contains("-")) {
                fl += p + " & ";
              }
            }
            if (fl.endsWith(" & ")) fl = fl.substring(0, fl.length()-3);
            String fr = ""; 
            for (String p : r.split(" & ")) {
              if (p.contains("-")) {
                fr += p + " & ";
              }
            }
            if (fr.endsWith(" & ")) fr = fr.substring(0, fr.length()-3);
            fl = fl.replaceAll("\\+", "/").replaceAll("-", "\\+").replaceAll("/", "-");
            if (!fl.isEmpty() && !fr.isEmpty() && equals(fl.trim(), fr.trim())) {
              return true;
            }
          }
        }
        for (String lb : Lops) {
          for (String rb : Rops) {
            for (String l : lb.split(" or ")) {
              for (String r : rb.split(" or ")) {
                l =l.split("& \\{")[0];
                r =r.split("& \\{")[0];
                l=format(l); r=format(r);
                String fl = ""; 
                for (String p : l.split(" & ")) {
                  if (!p.contains("-")) {
                    fl += p + " & ";
                  }
                }
                if (fl.endsWith(" & ")) fl = fl.substring(0, fl.length()-3);
                String fr = ""; 
                for (String p : r.split(" & ")) {
                  if (r.contains("-")) {
                    fr += p + " & ";
                  }
                }
                if (fr.endsWith(" & ")) fr = fr.substring(0, fr.length()-3);
                fl = fl.replaceAll("\\+", "/").replaceAll("-", "\\+").replaceAll("/", "-");
                if (!fl.isEmpty() && !fr.isEmpty() && equals(fl.trim(), fr.trim())) {
                  return true;
                }
              }
            }
          }
        }
        
        for (String lb : Lops) {
          for (String l : lb.split(" or ")) {
            l =l.split("& \\{")[0];
            while (l.contains("[")) {
              int start = l.indexOf("[");
              int end = 0;
              int numC = 1, num = 0;
              for (int i =start+1;i < l.length(); i++) {
                if (l.charAt(i) == '[') numC++;
                else if (l.charAt(i) == ']') num++;
                if (numC==num) {
                  end=i+1; break;
                }
              }
              Lcosts.add(l.substring(start,end==0?l.length():end));
              l = l.substring(0, start)+l.substring(end==0?l.length():end);
            }
            l=format(l);
            String fl = ""; 
            for (String p : l.split(" & ")) {
              if (!p.contains("-")) {
                fl += p + " & ";
              }
            }
            if (fl.endsWith(" & ")) fl = fl.substring(0, fl.length()-3);
            fl = fl.replaceAll("\\+", "/").replaceAll("-", "\\+").replaceAll("/", "-");
            for (String r : rr.split(" or ")) {
              r=format(r);
              String fr = ""; 
              for (String p : r.split(" & ")) {
                if (p.contains("-")) {
                  fr += p + " & ";
                }
              }
              if (fr.endsWith(" & ")) fr = fr.substring(0, fr.length()-3);
              if (!fl.isEmpty() && !fr.isEmpty() && equals(fl.trim(), fr.trim())) {
                return true;
              }
            }
          }
        }

        for (String rb : Rops) {
          for (String r : rb.split(" or ")) {
            r =r.split("& \\{")[0];
            while (r.contains("[")) {
              int start = r.indexOf("[");
              int end = 0;
              int numC = 1, num = 0;
              for (int i =start+1;i < r.length(); i++) {
                if (r.charAt(i) == '[') numC++;
                else if (r.charAt(i) == ']') num++;
                if (numC==num) {
                  end=i+1; break;
                }
              }
              Rcosts.add(r.substring(start,end==0?r.length():end));
              r = r.substring(0, start)+r.substring(end==0?r.length():end);
            }
            r=format(r);
            String fr = ""; 
            for (String p : r.split(" & ")) {
              if (p.contains("-")) {
                fr += p + " & ";
              }
            }
            if (fr.endsWith(" & ")) fr = fr.substring(0, fr.length()-3);
            for (String l : lr.split(" or ")) {
              l=format(l);
              String fl = ""; 
              for (String p : l.split(" & ")) {
                if (!p.contains("-")) {
                  fl += p + " & ";
                }
              }
              if (fl.endsWith(" & ")) fl = fl.substring(0, fl.length()-3);
              fl = fl.replaceAll("\\+", "/").replaceAll("-", "\\+").replaceAll("/", "-");
              if (!fl.isEmpty() && !fr.isEmpty() && equals(fl.trim(), fr.trim())) {
                return true;
              }
            }
          }
        }
      }
    }
    return false;
  }
  
  private static boolean checkLR(String left, String right) {
    if (left.toLowerCase().trim().equals(right.toLowerCase().trim())) return false;
    if (left.toLowerCase().equals("a") && right.equals("is")) return false;
    if (left.toLowerCase().equals("a") || right.toLowerCase().equals("a")) return true;
    if (dict.getSubscript(left.toLowerCase().trim()).contains("n-u") && dict.getSubscript(right.toLowerCase().trim()).contains("m")) return false;
    if (dict.getSubscript(right.toLowerCase().trim()).contains("n-u") && dict.getSubscript(left.toLowerCase().trim()).contains("m")) return false;
    if (dict.getSubscript(left.toLowerCase().trim()).contains("n-u") && dict.getSubscript(right.toLowerCase().trim()).contains("f")) return false;
    if (dict.getSubscript(right.toLowerCase().trim()).contains("n-u") && dict.getSubscript(left.toLowerCase().trim()).contains("f")) return false;
    if (left.toLowerCase().equals("a") && dict.getSubscript(right).size()==1 && dict.getSubscript(right).get(0).equals("v"))
      return false;
    return true;
  }
  
  private static Object[] connectsIdx(String left, String right, boolean isLeft) {
    if (!checkLR(left, right)) return new Object[] {false, 0};
    ArrayList<Rule> leftList = dict.getRule(left), rightList = dict.getRule(right);
    if (leftList.size()==0) {
      System.err.println("Word '" + left + "' not found in dictionary.");
      System.exit(0);
    }
    if (rightList.size()==0) {
      System.err.println("Word '" + right + "' not found in dictionary.");
      System.exit(0);
    }
    for (Rule leftRule : leftList) {
      for (Rule rightRule : rightList) {
        String lr = leftRule.toString();
        String rr = rightRule.toString();
        lr=beforeNull(lr);
        rr=beforeNull(rr);
        lr = replaceNull(lr);
        rr = replaceNull(rr);
        
        ArrayList<String> Lops = new ArrayList<>(), Rops = new ArrayList<>(), 
            Lcosts = new ArrayList<>(), Rcosts = new ArrayList<>();
        while (lr.contains("{")) {
          int start = lr.indexOf("{");
          int end = 0;
          int numC = 1, num = 0;
          for (int i =start+1;i < lr.length(); i++) {
            if (lr.charAt(i) == '{') numC++;
            else if (lr.charAt(i) == '}') num++;
            if (numC==num) {
              end=i+1; break;
            }
          }
          Lops.add(lr.substring(start,end));
          lr = lr.substring(0, start)+lr.substring(end);
        }
        lr = fixString(lr);
        
        while (rr.contains("{")) {
          int start = rr.indexOf("{");
          int end = 0;
          int numC = 1, num = 0;
          for (int i =start+1;i < rr.length(); i++) {
            if (rr.charAt(i) == '{') numC++;
            else if (rr.charAt(i) == '}') num++;
            if (numC==num) {
              end=i+1; break;
            }
          }
          Rops.add(rr.substring(start,end));
          rr = rr.substring(0, start)+rr.substring(end);
        }
        rr = fixString(rr);
        
        int li = 0, ri = 0;
        for (String l : lr.split(" or ")) {
          ri=0;
          li++;
          for (String r : rr.split(" or ")) {
            ri++;
            l=format(l); r=format(r);
            String fl = ""; 
            for (String p : l.split(" & ")) {
              if (!p.contains("-")) {
                fl += p + " & ";
              }
            }
            if (fl.endsWith(" & ")) fl = fl.substring(0, fl.length()-3);
            String fr = ""; 
            for (String p : r.split(" & ")) {
              if (p.contains("-")) {
                fr += p + " & ";
              }
            }
            if (fr.endsWith(" & ")) fr = fr.substring(0, fr.length()-3);
            fl = fl.replaceAll("\\+", "/").replaceAll("-", "\\+").replaceAll("/", "-");
            if (!fl.isEmpty() && !fr.isEmpty() && equals(fl.trim(), fr.trim())) {
              return new Object[] {true, isLeft? li : ri};
            }
          }
        }
        li = 0; ri = 0;
        for (String lb : Lops) {
          ri=0;
          li++;
          for (String rb : Rops) {
            ri++;
            for (String l : lb.split(" or ")) {
              for (String r : rb.split(" or ")) {
                l =l.split("& \\{")[0];
                r =r.split("& \\{")[0];
                l=format(l); r=format(r);
                String fl = ""; 
                for (String p : l.split(" & ")) {
                  if (!p.contains("-")) {
                    fl += p + " & ";
                  }
                }
                if (fl.endsWith(" & ")) fl = fl.substring(0, fl.length()-3);
                String fr = ""; 
                for (String p : r.split(" & ")) {
                  if (r.contains("-")) {
                    fr += p + " & ";
                  }
                }
                if (fr.endsWith(" & ")) fr = fr.substring(0, fr.length()-3);
                fl = fl.replaceAll("\\+", "/").replaceAll("-", "\\+").replaceAll("/", "-");
                if (!fl.isEmpty() && !fr.isEmpty() && equals(fl.trim(), fr.trim())) return new Object[] {true, isLeft? li : ri};
              }
            }
          }
        }
        li = 0; ri = 0;
        for (String lb : Lops) {
          ri=0;
          li++;
          for (String l : lb.split(" or ")) {
            l =l.split("& \\{")[0];
            while (l.contains("[")) {
              int start = l.indexOf("[");
              int end = 0;
              int numC = 1, num = 0;
              for (int i =start+1;i < l.length(); i++) {
                if (l.charAt(i) == '[') numC++;
                else if (l.charAt(i) == ']') num++;
                if (numC==num) {
                  end=i+1; break;
                }
              }
              Lcosts.add(l.substring(start,end==0?l.length():end));
              l = l.substring(0, start)+l.substring(end==0?l.length():end);
            }
            l=format(l);
            String fl = ""; 
            for (String p : l.split(" & ")) {
              if (!p.contains("-")) {
                fl += p + " & ";
              }
            }
            if (fl.endsWith(" & ")) fl = fl.substring(0, fl.length()-3);
            fl = fl.replaceAll("\\+", "/").replaceAll("-", "\\+").replaceAll("/", "-");
            for (String r : rr.split(" or ")) {
              ri++;
              r=format(r);
              String fr = ""; 
              for (String p : r.split(" & ")) {
                if (p.contains("-")) {
                  fr += p + " & ";
                }
              }
              if (fr.endsWith(" & ")) fr = fr.substring(0, fr.length()-3);
              if (!fl.isEmpty() && !fr.isEmpty() && equals(fl.trim(), fr.trim())) return new Object[] {true, isLeft? li : ri};
            }
          }
        }
        li = 0; ri = 0;
        for (String rb : Rops) {
          li = 0;
          ri++;
          for (String r : rb.split(" or ")) {
            r =r.split("& \\{")[0];
            while (r.contains("[")) {
              int start = r.indexOf("[");
              int end = 0;
              int numC = 1, num = 0;
              for (int i =start+1;i < r.length(); i++) {
                if (r.charAt(i) == '[') numC++;
                else if (r.charAt(i) == ']') num++;
                if (numC==num) {
                  end=i+1; break;
                }
              }
              Rcosts.add(r.substring(start,end==0?r.length():end));
              r = r.substring(0, start)+r.substring(end==0?r.length():end);
            }
            r=format(r);
            String fr = ""; 
            for (String p : r.split(" & ")) {
              if (p.contains("-")) {
                fr += p + " & ";
              }
            }
            if (fr.endsWith(" & ")) fr = fr.substring(0, fr.length()-3);
            for (String l : lr.split(" or ")) {
              li++;
              l=format(l);
              String fl = ""; 
              for (String p : l.split(" & ")) {
                if (!p.contains("-")) {
                  fl += p + " & ";
                }
              }
              if (fl.endsWith(" & ")) fl = fl.substring(0, fl.length()-3);
              fl = fl.replaceAll("\\+", "/").replaceAll("-", "\\+").replaceAll("/", "-");
              if (!fl.isEmpty() && !fr.isEmpty() && equals(fl.trim(), fr.trim())) {
                return new Object[] {true, isLeft? li : ri};
              }
            }
          }
        }
      }
    }
    return new Object[] {false, 0};
  }
  
  private static String replaceNull(String lr) {
    while (lr.contains("()")) {
      int idx = lr.indexOf("()");
      int end = lr.indexOf(" ", idx);
      if (lr.charAt(end-1)=='}') return lr;
      if (lr.charAt(end-1)==')') {
        for (int i = end-2; i >= 0; i--) {
          if (lr.charAt(i)!=')') {
            end=i+2; break;
          }
        }
        int num = 1;
        int numC = 0;
        int finish = 0;
        for (int i = end-2; i>=0; i--) {
          if (lr.charAt(i)==')') num++;
          else if (lr.charAt(i)=='(') numC++;
          if (numC==num) {
            finish=i; break;
          }
        }
        String total = lr.substring(finish,end);
        lr = lr.replace(total, "{"+total.substring(1,total.lastIndexOf("or"))+"}");
      } else {
        int space1 = 0;
        for (int i = end-1; i>=0; i--) {
          if (lr.charAt(i)==' ') {
            space1 = i; break;
          }
        }
        if (lr.charAt(space1-4)==')' || lr.charAt(space1-4)==']') {
          char c = lr.charAt(space1-4);
          char oc;
          if (c == ')') oc = '('; 
          else oc = '[';
          int finish = 0;
          int num = 1;
          int numC = 0;
          for (int i = space1-5; i>=0; i--) {
            if (lr.charAt(i)==c) num++;
            else if (lr.charAt(i)==oc) numC++;
            if (numC==num) {
              finish=i; break;
            }
          }
          lr = lr.replace(lr.substring(finish, end), "{"+lr.substring(finish+1,space1-4)+"}");
        } else if (lr.charAt(space1-5)==')' || lr.charAt(space1-5)==']') {
          char c = lr.charAt(space1-5);
          char oc;
          if (c == ')') oc = '('; 
          else oc = '[';
          int finish = 0;
          int num = 1;
          int numC = 0;
          for (int i = space1-6; i>=0; i--) {
            if (lr.charAt(i)==c) num++;
            else if (lr.charAt(i)==oc) numC++;
            if (numC==num) {
              finish=i; break;
            }
          }
          lr = lr.replace(lr.substring(finish, end), "{"+lr.substring(finish+1,space1-4)+"}");
        } else {
          int space2 = 0;
          for (int i = space1-5; i>=0; i--) {
            if (lr.charAt(i)==' ') {
              space2 = i; break;
            }
          }
          if (lr.charAt(space2+1)=='(') space2++;
          int fin = lr.indexOf(' ',space2+1);
          lr = lr.replace(lr.substring(space2+1, end), "{"+lr.substring(space2+1, fin)+"}");
        }
      }
    }
    return lr;
  }
  
  private static String beforeNull(String lr) {
    lr = fix(lr,"([()] & ", "(");
    lr = fix(lr, "  or ", " or ");
    lr = fix(lr, "or ()", "or [()]");
    return lr;
  }
  
  private static String fixString(String lr) {
    lr=fix(lr,"[ & ]", "");  lr=fix(lr,"( & )", "");
    lr=fix(lr,"[ or ]", ""); lr=fix(lr,"( or )", "");
    lr=fix(lr,"( or ", "("); lr=fix(lr,"( & ", "(");
    lr=fix(lr," or )", ")"); lr=fix(lr," & )", ")");
    lr=fix(lr,"[ or ", "["); lr=fix(lr,"[ & ", "[");
    lr=fix(lr," or ]", "]"); lr=fix(lr," & ]", "]");
    lr=fix(lr,"&  &", "&");  lr=fix(lr,"or  or", "or");
    return lr;
  }
  
  private static String fix(String lr, String reg, String rep) {
    while (lr.contains(reg)) lr=lr.replace(reg, rep);
    return lr;
  }
  
  private static String format(String l) {
    return l.replace("(", "").replace(")", "").replace("[", "").replace("]", "").replace("}", "").replace("{", "").trim();
  }
  
  public static HashSet<String> generateSentence(String[] elements) {
    boolean not = false;
    boolean now = false;
    HashSet<String> ret = new HashSet<>();
    int n = elements.length;
    int[] indexes = new int[n];
    for (int i = 0; i < n; i++) {
      indexes[i] = 0;
    }
    ArrayList<String> w = new ArrayList<>(Arrays.asList(elements));
    if (w.contains("not") && w.contains("a")) {
      not = true;
      w.remove("not");
    }
    if (w.contains("now") && w.contains("a")) {
      now = true;
      w.remove("now");
    }
    if (not || now) {
      int i = 0;
      String[] input = new String[w.size()];
      for (String word : w) {
        input[i] = word;
        i++;
      }
      if (now && !not) {
        if (check(input) && isValid(input)) {
          String str = makeSentence(input);
          int nowId = 0;
          String[] parts = str.split(" ");
          for (int m = 0; m < parts.length; m++) {
            if (parts[m].equals("a")) {
              nowId = str.indexOf(" a") + 2 + parts[m + 1].length();
            }
          }
          String str3 = str.substring(0, nowId) + " now" + str.substring(nowId);
          ret.add(str3);
        }
      }
      if (not && !now) {
        if (check(input) && isValid(input)) {
          String str = makeSentence(input);
          str = str.replace(" a", " not a");
          ret.add(str);
        }
      }
      if (not && now) {
        if (check(input) && isValid(input)) {
          String str = makeSentence(input);
          int nowId = 0;
          String[] parts = str.split(" ");
          for (int m = 0; m < parts.length; m++) {
            if (parts[m].equals("a")) {
              nowId = str.indexOf(" a") + 2 + parts[m + 1].length();
            }
          }
          String str3 = str.substring(0, nowId) + " now" + str.substring(nowId);
          str3 = str3.replace(" a", " not a");
          ret.add(str3);
        }
      }
    } else {
      if (check(elements) && isValid(elements)) {
        ret.add(makeSentence(elements));
      }
    }
    int i = 0;
    while (i < n) {
      if (indexes[i] < i) {
        swap(elements, i % 2 == 0 ? 0 : indexes[i], i);
        ArrayList<String> w2 = new ArrayList<>(Arrays.asList(elements));
        if (w2.contains("not") && w2.contains("a")) {
          w2.remove("not");
        }
        if (w2.contains("now") && w2.contains("a")) {
          w2.remove("now");
        }
        if (not || now) {
          int id = 0;
          String[] input = new String[w2.size()];
          for (String word : w2) {
            input[id] = word;
            id++;
          }
          if (now && !not) {
            if (check(input) && isValid(input)) {
              String str = makeSentence(input);
              int nowId = 0;
              String[] parts = str.split(" ");
              for (int m = 0; m < parts.length; m++) {
                if (parts[m].equals("a") && str.indexOf(" a") != -1) {
                  nowId = str.indexOf(" a") + 3 + parts[m + 1].length() 
                      - ((parts[m+1].contains(".") || parts[m+1].contains("?"))? 1 : 0);
                  break;
                }
              }
              if (nowId == 0) {
                ret.add("Now " + str.toLowerCase());
              } else {
                String str3 = str.substring(0, nowId) + " now" + str.substring(nowId);
                ret.add(str3);
              }
            }
          }
          if (not && !now) {
            if (check(input) && isValid(input)) {
              String str = makeSentence(input);
              str = str.replace(" a", " not a");
              ret.add(str);
            }
          }
          if (not && now) {
            if (check(input) && isValid(input)) {
              String str = makeSentence(input);
              int nowId = 0;
              String[] parts = str.split(" ");
              for (int m = 0; m < parts.length; m++) {
                if (parts[m].equals("a")) {
                  nowId = str.indexOf(" a") + 2 + parts[m + 1].length();
                }
              }
              String str3 = str.substring(0, nowId) + " now" + str.substring(nowId);
              str3 = str3.replace(" a", " not a");
              ret.add(str3);
            }
          }
        } else {
          if (check(elements) && isValid(elements)) {
            ret.add(makeSentence(elements));
          }
        }
        indexes[i]++;
        i = 0;
      } else {
        indexes[i] = 0;
        i++;
      }
    }
    return ret;
  }
  
  private static boolean isValid(String[] input) {
    if (idx(input, "A") != -1 && idx(input, "A") != 0) return false;
    if ((dict.getSubscript(input[0]).contains("v") || dict.getSubscript(input[0]).contains("v-d")) && !(dict.getSubscript(input[0]).contains("n") 
        || dict.getSubscript(input[0]).contains("n-u"))) return false;
    for (int i = 0; i < input.length; i++) {
      if (!input[i].equals("A")) {
        input[i] = input[i].toLowerCase().trim();
      }
    }
    String last = input[input.length-1].toLowerCase().trim();
    if (last.equals("a") || (dict.getSubscript(last).size() > 0 && (!dict.getSubscript(last).contains("n") && !dict.getSubscript(last).contains("r") && !dict.getSubscript(last).contains("n-u")))) return false;
    outer: for (int i = 0; i < input.length - 1; i++) {
      String left = input[i];
      String right = input[i + 1];
      if (left.toLowerCase().trim().equals(right.toLowerCase().trim())) return false;
      if ((dict.getRule(left.toLowerCase()).get(0).toString().equals(dict.getRule("sawed").get(0).toString())
          || (dict.getRule(left.toLowerCase()).get(0).toString().equals(dict.getRule("writes").get(0).toString()) 
              && contains(input, "to"))
          || left.equals("saw"))
          && (idx(input, "with") == i+2 || idx(input, "with") == i+3)) {
        int idx = idx(input, "with");
        if (idx == i + 2) {
          i = idx-1;
          if (connectsLeft(left, right, input[i+1]))
            continue outer;
        } else {
          i = idx-1;
          if (right.equals("to")) {
            if (connectsLeft(left, right, input[i+1]) && connects(right, input[i]))
              continue outer;
          } else if (right.equals("the")) {
            if (connectsAll(left, right, input[i], input[i+1]))
              continue outer;
          }
        }
      } else if ((right.equals("a") || right.equals("the")) && i + 2 < input.length) {
        i++;
        if (connects(left, right, input[i + 1])) {
          continue outer;
        }
      } else if (right.equals("on") && i >= 2 && (input[i - 1].equals("a") || input[i - 1].equals("with"))) {
        if (input[i - 1].equals("a") && i >= 3) {
          if (connectsLeft(input[i - 3], input[i - 2], right))
            continue outer;
        } else {
          if (connectsLeft(input[i - 2], input[i - 1], right))
            continue outer;
          else {
            if (i >= 4 && connectsMin(input[i-4], right)) continue outer;
          }
        }
      } else if (left.equals("wants") && i + 2 < input.length) {
        if (right.equals("to")) {
          i += 2;
          if (connectsLeft("wants", "to", input[i]))
            continue outer;
        } else if (input[i + 2].equals("to")) {
          if (i + 3 < input.length) {
            i += 3;
            if (connectsFour(left, right, input[i - 1], input[i]))
              continue outer;
          }
        } else if (input[i + 3].equals("to")) {
          if (i + 4 < input.length) {
            i += 4;
            if (connectsFour(left, input[i-2], input[i - 1], input[i])) {
              continue outer;
            }
          }
        }
      } else {
        if (connects(left, right)) {
          continue outer;
        }
        if (right.equals("with")) {
          int idx;
          for (idx = 0; idx < input.length; idx++) {
            if (input[idx].equals("on")) {
              break;
            }
          }
          if (idx > i) {
            if (connectsLeft(left, right, "on"))
              continue outer;
          }
        }
      }
      return false;
    }
    return true;
  }
  
  private static boolean connectsMin(String left, String right) {
    ArrayList<Rule> leftList = dict.getRule(left), rightList = dict.getRule(right);
    if (leftList.size()==0) {
      System.err.println("Word '" + left + "' not found in dictionary.");
      System.exit(0);
    }
    if (rightList.size()==0) {
      System.err.println("Word '" + right + "' not found in dictionary.");
      System.exit(0);
    }
    for (Rule leftRule : leftList) {
      for (Rule rightRule : rightList) {
        for (Disjunct dl : leftRule.getDisjuncts()) {
          for (Disjunct dr : rightRule.getDisjuncts()) {
            String wl = "";
            String wr = "";
            if (dl.getConnectors().size() > 1) {
              for (int ci = 0; ci < dl.getConnectors().size() - 1; ci++) {
                String c = dl.getConnectors().get(ci);
                if (!c.contains("-")) {
                  wl += c + " & ";
                }
              }
              String c = dl.getConnectors().get(dl.getConnectors().size() - 1);
              if (!c.contains("-")) {
                wl += c;
              } else {
                if (wl.contains("&"))
                  wl = wl.substring(0, wl.length() - 3);
              }
            } else {
              wl = dl.getConnectors().get(0);
            }
            if (dr.getConnectors().size() > 1) {
              for (int ci = 0; ci < dr.getConnectors().size() - 1; ci++) {
                String c = dr.getConnectors().get(ci);
                if (c.contains("-")) {
                  wr += c + " & ";
                }
              }
              String c = dr.getConnectors().get(dr.getConnectors().size() - 1);
              if (c.contains("-")) {
                wr += c;
              } else {
                if (wr.contains("&"))
                  wr = wr.substring(0, wr.length() - 3);
              }
            } else {
              wr = dr.getConnectors().get(0);
            }
            for (String lp : wl.split(" & ")) {
              for (String rp : wr.split(" & ")) {
                String wlu = lp.replaceAll("\\+", "/").replaceAll("-", "\\+").replaceAll("/", "-");
                if (equals(wlu, rp)) {
                  return true;
                }
              }
            }
          }
        }
      }
    }
    return false;
  }

  private static boolean connects(String left, String mid, String right) {
    if (left.toLowerCase().equals(mid.toLowerCase()) || left.toLowerCase().equals(right.toLowerCase())
        || mid.toLowerCase().equals(right.toLowerCase())) return false;
    if (mid.toLowerCase().equals("a") && right.equals("is")) return false;
    // TODO: Fix this - left and mid should be part of the same rule in right
    Object[] leftRight = connectsIdx(left, right, false);
    Object[] midRight = connectsIdx(mid, right, false);
    return (boolean) leftRight[0] && (boolean) midRight[0] && (int) leftRight[1] < (int) midRight[1];
  }

  private static boolean connectsLeft(String left, String mid, String right) {
    if (left.toLowerCase().equals(mid.toLowerCase()) || left.toLowerCase().equals(right.toLowerCase())
        || mid.toLowerCase().equals(right.toLowerCase())) return false;
    Object[] leftMid = connectsIdx(left, mid, true);
    Object[] leftRight = connectsIdx(left, right, true);
    return (boolean) leftMid[0] && (boolean) leftRight[0] && (int) leftMid[1] < (int) leftRight[1];
  }

  private static boolean connectsFour(String left, String mid, String right, String next) {
    ArrayList<Rule> leftList = dict.getRule(left), midList=dict.getRule(mid), rightList = dict.getRule(right), nextList = dict.getRule(next);
    if (leftList.size()==0) {
      System.err.println("Word '" + left + "' not found in dictionary.");
      System.exit(0);
    }
    if (midList.size()==0) {
      System.err.println("Word '" + mid + "' not found in dictionary.");
      System.exit(0);
    }
    if (rightList.size()==0) {
      System.err.println("Word '" + right + "' not found in dictionary.");
      System.exit(0);
    }
    if (nextList.size()==0) {
      System.err.println("Word '" + next + "' not found in dictionary.");
      System.exit(0);
    }
    boolean midTrue = false;
    boolean rightTrue = false;
    boolean nextTrue = false;
    int rightId = 0;
    int midId = 0;
    int nextId = 0;
    for (Rule leftRule : leftList) {
      for (Rule midRule : midList) {
        for (Rule rightRule : rightList) {
          for (Rule nextRule : nextList) {
            for (Disjunct dl : leftRule.getDisjuncts()) {
              String wl = "";
              if (dl.getConnectors().size() > 1) {
                for (int ci = 0; ci < dl.getConnectors().size() - 1; ci++) {
                  String c = dl.getConnectors().get(ci);
                  if (!c.contains("-")) {
                    wl += c + " & ";
                  }
                }
                String c = dl.getConnectors().get(dl.getConnectors().size() - 1);
                if (!c.contains("-")) {
                  wl += c;
                } else {
                  if (wl.contains("&"))
                    wl = wl.substring(0, wl.length() - 3);
                }
              } else {
                wl = dl.getConnectors().get(0);
              }
              if (!wl.contains("&"))
                continue;
              for (Disjunct dr : rightRule.getDisjuncts()) {
                for (Disjunct dm : midRule.getDisjuncts()) {
                  for (Disjunct dn : nextRule.getDisjuncts()) {
                    String[] parts = wl.split(" & ");
                    for (int idp = 0; idp < parts.length; idp++) {
                      String part = parts[idp];
                      String wr = "";
                      if (dr.getConnectors().size() > 1) {
                        for (int ci = 0; ci < dr.getConnectors().size() - 1; ci++) {
                          String c = dr.getConnectors().get(ci);
                          if (c.contains("-")) {
                            wr += c + " & ";
                          }
                        }
                        String c = dr.getConnectors().get(dr.getConnectors().size() - 1);
                        if (c.contains("-")) {
                          wr += c;
                        } else {
                          if (wr.contains("&"))
                            wr = wr.substring(0, wr.length() - 3);
                        }
                      } else {
                        wr = dr.getConnectors().get(0);
                      }
                      String wm = "";
                      if (dm.getConnectors().size() > 1) {
                        for (int ci = 0; ci < dm.getConnectors().size() - 1; ci++) {
                          String c = dm.getConnectors().get(ci);
                          if (c.contains("-")) {
                            wm += c + " & ";
                          }
                        }
                        String c = dm.getConnectors().get(dm.getConnectors().size() - 1);
                        if (c.contains("-")) {
                          wm += c;
                        } else {
                          if (wm.contains("&"))
                            wm = wm.substring(0, wm.length() - 3);
                        }
                      } else {
                        wm = dm.getConnectors().get(0);
                      }
                      String wn = "";
                      if (dn.getConnectors().size() > 1) {
                        for (int ci = 0; ci < dn.getConnectors().size() - 1; ci++) {
                          String c = dn.getConnectors().get(ci);
                          if (c.contains("-")) {
                            wn += c + " & ";
                          }
                        }
                        String c = dn.getConnectors().get(dn.getConnectors().size() - 1);
                        if (c.contains("-")) {
                          wn += c;
                        } else {
                          if (wn.contains("&"))
                            wn = wn.substring(0, wn.length() - 3);
                        }
                      } else {
                        wn = dn.getConnectors().get(0);
                      }
                      String wru = wr.replaceAll("\\+", "/").replaceAll("-", "\\+").replaceAll("/", "-");
                      String wmu = wm.replaceAll("\\+", "/").replaceAll("-", "\\+").replaceAll("/", "-");
                      String wnu = wn.replaceAll("\\+", "/").replaceAll("-", "\\+").replaceAll("/", "-");
                      if (equals(wru, part) && !rightTrue) {
                        rightTrue = true;
                        rightId = idp;
                      }
                      if (equals(wmu, part) && !midTrue) {
                        midTrue = true;
                        midId = idp;
                      }
                      if (equals(wnu, part) && !nextTrue) {
                        nextTrue = true;
                        nextId = idp;
                      } else if (!nextTrue) {
                        ifc: if (wnu.contains("&")) {
                          String[] p = wnu.split(" & ");
                          for (String s : p) {
                            if (equals(s, part)) {
                              nextTrue = true;
                              nextId = idp;
                              break ifc;
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
    return rightTrue && midTrue && nextTrue && (midId < rightId) && (rightId < nextId);
  }
  
  private static boolean connectsAll(String left, String mid, String right, String next) {
    ArrayList<Rule> leftList = dict.getRule(left), midList=dict.getRule(mid), rightList = dict.getRule(right), nextList = dict.getRule(next);
    if (leftList.size()==0) {
      System.err.println("Word '" + left + "' not found in dictionary.");
      System.exit(0);
    }
    if (midList.size()==0) {
      System.err.println("Word '" + mid + "' not found in dictionary.");
      System.exit(0);
    }
    if (rightList.size()==0) {
      System.err.println("Word '" + right + "' not found in dictionary.");
      System.exit(0);
    }
    if (nextList.size()==0) {
      System.err.println("Word '" + next + "' not found in dictionary.");
      System.exit(0);
    }
    boolean one = false;
    boolean two = false;
    for (Rule leftRule : leftList) {
      for (Disjunct dl : leftRule.getDisjuncts()) {
        String wl = "";
        if (dl.getConnectors().size() > 1) {
          for (int ci = 0; ci < dl.getConnectors().size() - 1; ci++) {
            String c = dl.getConnectors().get(ci);
            if (!c.contains("-")) {
              wl += c + " & ";
            }
          }
          String c = dl.getConnectors().get(dl.getConnectors().size() - 1);
          if (!c.contains("-")) {
            wl += c;
          } else {
            if (wl.contains("&"))
              wl = wl.substring(0, wl.length() - 3);
          }
        } else {
          wl = dl.getConnectors().get(0);
        }
        if (!wl.contains("&")) continue;
        String[] parts = wl.split(" & ");
        for (int idp = 0; idp < parts.length; idp++) {
          String part = parts[idp];
          Rule r = new Rule();
          r.addWord(part);
          dict.addWord(new Word(part.toLowerCase(), r));
          if (connects(part, mid, right)) one = true;
          if (connects(part, next)) two = true;
        }
      }
      if (one&&two) return true;
    }
    return one && two;
  }
  
  private static boolean equals(String wlu, String wr) {
    if (wlu.equals(wr)) {
      return true;
    }
    if (wlu.contains("*")) {
      int idx = wlu.indexOf("*");
      int lid = wlu.lastIndexOf("*");
      if (wr.length() == wlu.length()) {
        if (wlu.substring(0, idx).equals(wr.substring(0, idx)) 
            && wlu.substring(lid+1).equals(wr.substring(lid+1))) {
          return true;
        }
      } else {
        if (idx+1>wr.length() && wlu.substring(0, idx).equals(wr.substring(0, Math.min(wr.length(), idx)))) return true;
      }
    } 
    if (wr.contains("*")) {
      int idx = wr.indexOf("*");
      int lid = wr.lastIndexOf("*");
      if (wr.length() == wlu.length()) {
        if (wlu.substring(0, idx).equals(wr.substring(0, idx)) 
            && wlu.substring(lid+1).equals(wr.substring(lid+1))) {
          return true;
        }
      } else {
        if (idx+1>wlu.length() && wlu.substring(0, Math.min(wlu.length(), idx)).equals(wr.substring(0, idx))) {
          return true;
        }
      }
    }
    wr=wr.replace("-", "");
    wlu=wlu.replace("-", "");
    if (wlu.contains("&") || wr.contains("&")) return false;
    if (wlu.length() < wr.length()) {
      if (wlu.equals(wr.substring(0, wlu.length()))) return true;
    }
    if (wr.length() < wlu.length()) {
      if (wr.equals(wlu.substring(0, wr.length()))) return true;
    }
    return false;
  }

  public static List<String[]> processSentences(String path) throws IOException {
    try {
      Path p;
      if (System.getProperty("user.dir").endsWith("src")) {
        p = Paths.get(Paths.get("test/java/org/aigents/nlp/gen/" + path).toAbsolutePath().toString());
      } else {
        p = Paths.get(Paths.get("src/test/java/org/aigents/nlp/gen/" + path).toAbsolutePath().toString());
      }
      File f = p.toFile();
      List<String> sentences = Files.readAllLines(f.toPath());
      Iterator<String> it = sentences.iterator();
      while (it.hasNext()) {
        if (it.next().isEmpty())
          it.remove();
      }
      List<String[]> words = new ArrayList<>();
      for (String sentence : sentences) {
        String[] w = sentence.split(" ");
        w[w.length - 1] = w[w.length - 1].substring(0, w[w.length - 1].length() - 1);
        words.add(w);
      }
      return words;
    } catch (Exception e) {
      return null;
    }
  }
  
  public static List<String> getList(String path) throws IOException {
    try {
      Path p;
      if (path.contains("/4.0.dict")) {
        if (System.getProperty("user.dir").endsWith("src")) {
          p = Paths.get(Paths.get("../data/" + path).toAbsolutePath().toString());
        } else {
          p = Paths.get(Paths.get("data/" + path).toAbsolutePath().toString());
        }
      } else {
        if (System.getProperty("user.dir").endsWith("src")) {
          p = Paths.get(Paths.get("test/java/org/aigents/nlp/gen/" + path).toAbsolutePath().toString());
        } else {
          p = Paths.get(Paths.get("src/test/java/org/aigents/nlp/gen/" + path).toAbsolutePath().toString());
        }
      }
      File f = p.toFile();
      List<String> sentences = Files.readAllLines(f.toPath());
      Iterator<String> it = sentences.iterator();
      while (it.hasNext()) {
        if (it.next().isEmpty())
          it.remove();
      }
      return sentences;
    } catch (Exception e) {
      return null;
    }
  }
  
  private static boolean contains(String[] input, String str) {
    for (String s : input) {
      if (s.equals(str))
        return true;
    }
    return false;
  }

  private static int idx(String[] input, String str) {
    int i = -1;
    for (String s : input) {
      i++;
      if (s.equals(str)) return i;
    }
    return -1;
  }
  
  private static void swap(String[] input, int a, int b) {
    String tmp = input[a];
    input[a] = input[b];
    input[b] = tmp;
  }

  private static String makeSentence(String[] arr) {
    String ret = "";
    for (int i = 0; i < arr.length - 1; i++) {
      ret += arr[i] + " ";
    }
    ret += arr[arr.length - 1] + (arr[0].equals("is") || arr[0].equals("was")? "?" : ".");
    ret = ret.toLowerCase();
    ret = ret.substring(0,1).toUpperCase() + ret.substring(1);
    return ret;
  }
  
  // TODO: Finish
  private static boolean check(String[] input) {
    return true;
  }
}