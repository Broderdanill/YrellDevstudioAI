import java.util.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import se.yrell.ai.core.*;
public final class TaskExportTest {
    static int checks=0;
    static void yes(boolean v,String m){if(!v)throw new AssertionError(m);checks++;}
    public static void main(String[]args)throws Exception{
        List<TaskTemplates.Template> def=TaskTemplates.defaults();
        yes(def.size()==4,"4 initial templates");
        String json=TaskTemplates.encode(def);
        List<TaskTemplates.Template> copy=TaskTemplates.decode(json);
        yes(copy.size()==4&&copy.get(0).prompt().contains("{{OBJECTS}}"),"templates JSON roundtrip");
        yes(TaskTemplates.render("{{SERVER}} / {{OBJECTS}}",Map.of("SERVER","server1","OBJECTS","FORM: Test"))
            .equals("server1 / FORM: Test"),"variables substituted");
        yes(!TaskTemplates.render("{{MISSING}}",Map.of()).isBlank(),"undefined variables do not silently disappear");
        yes(TaskTemplates.slug("Å B! Test").equals("b-test"),"command-safe slug");
        Path tmp=Files.createTempDirectory("yrell-task-test-");
        try{
            Path nativeDef=Files.writeString(tmp.resolve("Sample.def"),"EXPORTARSCHEMA\n",StandardCharsets.UTF_8);
            TaskExporter.Request req=new TaskExporter.Request(tmp.resolve("jobs"),"MyAR",
                List.of("[Form: TEST1]","[Filter: TEST1:Test]"),"Definition snapshot foo",
                copy.get(0),TaskTemplates.DEFAULT_COMMON,"Include a field mapping table",
                true,List.of(nativeDef),List.of("mock warnings"));
            Path project=TaskExporter.export(req);
            yes(Files.isDirectory(project.resolve(".opencode/commands")),"opencode command folder");
            yes(Files.readString(project.resolve(".opencode/commands/yrell-task.md")).contains("$ARGUMENTS"),"slash command");
            yes(Files.readString(project.resolve("prompt.md")).contains("Include a field mapping table"),"user instructions");
            yes(Files.readString(project.resolve("prompt.md")).contains("[Form: TEST1]"),"selected objects");
            yes(Files.readString(project.resolve("definitions/context.md")).equals("Definition snapshot foo"),"definition snapshot");
            yes(Files.readString(project.resolve("definitions/Sample.def")).equals("EXPORTARSCHEMA\n"),"native DEF byte-for-byte");
            var meta=Json.object(Json.parse(Files.readString(project.resolve("manifest.json"))));
            yes(Boolean.FALSE.equals(meta.get("nativeDefAutoExport")),"correct native DEF capability flag");
            yes(Json.array(meta.get("nativeDefinitions")).size()==1,"DEF provenance in manifest");
            yes(Files.isDirectory(project.resolve("output")),"output folder exists");
            Path project2=TaskExporter.export(req);
            yes(!project.equals(project2),"two exports never overwrite");
            try {TaskExporter.export(new TaskExporter.Request(tmp.resolve("jobs"),"S",List.of("[Form: A]"),"x",copy.get(0),"","",true,
                List.of(tmp.resolve("not-found.def")),List.of()));throw new AssertionError("missing DEF accepted");}
            catch(java.io.IOException expected){checks++;}
        }finally {try(var files=Files.walk(tmp)){files.sorted(Comparator.reverseOrder()).forEach(p->{try{Files.deleteIfExists(p);}catch(Exception ignored){}});}}
        System.out.println("TaskExportTest: "+checks+" PASS");
    }
}
