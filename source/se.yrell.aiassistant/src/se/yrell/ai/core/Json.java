package se.yrell.ai.core;

import java.util.*;
/** Small dependency-free strict JSON serializer/parser for the two supported HTTP protocols. */
public final class Json {
    private Json() {}
    public static String write(Object value) {
        if(value==null) return "null";
        if(value instanceof CharSequence s) return quote(s.toString());
        if(value instanceof Boolean || value instanceof Number) return value.toString();
        if(value instanceof Map<?,?> map) {
            StringJoiner joiner=new StringJoiner(",","{","}");
            for (var entry:map.entrySet()) joiner.add(quote(String.valueOf(entry.getKey()))+":"+write(entry.getValue()));
            return joiner.toString();
        }
        if(value instanceof Iterable<?> values) {
            StringJoiner joiner=new StringJoiner(",","[","]");
            for(Object element:values) joiner.add(write(element));
            return joiner.toString();
        }
        throw new IllegalArgumentException("Not a JSON value: "+value.getClass().getName());
    }
    private static String quote(String s) {
        StringBuilder out=new StringBuilder("\"");
        for(int i=0;i<s.length();i++) {
            char c=s.charAt(i);
            switch(c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                default -> { if (c < 32) out.append(String.format("\\u%04x",(int)c)); else out.append(c); }
            }
        }
        return out.append('"').toString();
    }
    public static Object parse(String input) {
        Parser p = new Parser(input);
        Object value=p.value(); p.ws();
        if(p.pos!=p.s.length()) throw new IllegalArgumentException("Unexpected trailing JSON at "+p.pos);
        return value;
    }
    @SuppressWarnings("unchecked") public static Map<String,Object> object(Object v) {
        return v instanceof Map<?,?> m?(Map<String,Object>)m:Map.of();
    }
    @SuppressWarnings("unchecked") public static List<Object> array(Object v) {
        return v instanceof List<?> a?(List<Object>)a:List.of();
    }
    public static String text(Object v) { return v instanceof String s?s:""; }
    public static Object at(Object value,String... names) {
        for(String n:names) value=object(value).get(n);
        return value;
    }
    private static final class Parser {
        final String s; int pos;
        Parser(String s){this.s=Objects.requireNonNull(s);}
        void ws(){while(pos<s.length() && (s.charAt(pos)==' ' || s.charAt(pos)=='\n' || s.charAt(pos)=='\t' || s.charAt(pos)=='\r'))pos++;}
        char ch(){if(pos>=s.length())throw bad();return s.charAt(pos);}
        IllegalArgumentException bad(){return new IllegalArgumentException("Invalid JSON near index "+pos);}
        Object value(){ws();char c=ch(); if(c=='"')return string(); if(c=='{')return obj(); if(c=='[')return arr();
            if(s.startsWith("true",pos)){pos+=4;return true;} if(s.startsWith("false",pos)){pos+=5;return false;}
            if(s.startsWith("null",pos)){pos+=4;return null;} if(c=='-'||Character.isDigit(c))return number(); throw bad();}
        Map<String,Object> obj(){pos++;Map<String,Object> m=new LinkedHashMap<>();ws();if(ch()=='}'){pos++;return m;}
            while(true){ws();if(ch()!='"')throw bad();String k=string();ws();if(ch()!=':')throw bad();pos++;
                m.put(k,value());ws();char c=ch();pos++;if(c=='}')return m;if(c!=',')throw bad();}}
        List<Object> arr(){pos++;List<Object> a=new ArrayList<>();ws();if(ch()==']'){pos++;return a;}
            while(true){a.add(value());ws();char c=ch();pos++;if(c==']')return a;if(c!=',')throw bad();}}
        String string(){pos++;StringBuilder b=new StringBuilder();while(true){char c=ch();pos++;
            if(c=='"')return b.toString(); if(c=='\\'){char q=ch();pos++;switch(q){
                case '"' -> b.append('"'); case '\\' -> b.append('\\'); case '/' -> b.append('/');
                case 'n' -> b.append('\n'); case 'r' -> b.append('\r'); case 't' -> b.append('\t');
                case 'b' -> b.append('\b'); case 'f' -> b.append('\f');
                case 'u' -> {if(pos+4>s.length())throw bad();b.append((char)Integer.parseInt(s.substring(pos,pos+4),16));pos+=4;}
                default -> throw bad();
            }}else {if(c<32)throw bad();b.append(c);}}}
        Object number(){int start=pos;if(ch()=='-')pos++;while(pos<s.length()&&Character.isDigit(s.charAt(pos)))pos++;
            if(pos<s.length()&&s.charAt(pos)=='.'){pos++;while(pos<s.length()&&Character.isDigit(s.charAt(pos)))pos++;}
            if(pos<s.length()&&(s.charAt(pos)=='e'||s.charAt(pos)=='E')){pos++;if(pos<s.length()&&(s.charAt(pos)=='+'||s.charAt(pos)=='-'))pos++;while(pos<s.length()&&Character.isDigit(s.charAt(pos)))pos++;}
            try{return Double.valueOf(s.substring(start,pos));}catch(NumberFormatException e){throw bad();}}
    }
}
