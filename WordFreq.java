

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class WordFreq {
	final static String delim =" ,.;?!\"\'";
	final static String LANG = "English";
	
	static int zipCount = 0;
		 
	public static void main(String[] args) throws IOException{
		String path = "./docu";
		Map<Object,Integer> h = new HashMap<Object,Integer>();
		HashSet<String> txtList = new HashSet<String>();		
		
		Files.walk(Paths.get(path))
		.filter(Files::isReadable)
        .filter(Files::isRegularFile)
        .filter(file -> (file.toString().toLowerCase().endsWith(".txt") || file.toString().toLowerCase().endsWith(".zip")))
        .forEach(file -> {
			try {
				ProcessFile(file, h, txtList);
			} catch (IOException e) {
				
			}
		});     
		
        Map<Object,Integer> t = new TreeMap<Object,Integer>();
        for(Integer v : h.values()) 
        	add(t,v);
        
        System.out.println("Processed " + LANG +" txt files: "+ txtList.size());
        System.out.println("Decompressed zip files: "+ zipCount);
	}
	
	private static void ProcessFile(Path f,  Map<Object,Integer> h, HashSet<String> fl) throws IOException
	{	
		if (f.toString().toLowerCase().endsWith(".txt") && !fl.contains(f.toString().toLowerCase()))
		{
			BufferedReader in = new BufferedReader(new FileReader(f.toString()));
			String encod = checkTxt(in, LANG);
			if (!encod.isEmpty())
        	{
				BufferedReader inBuff = new BufferedReader(new FileReader(f.toString(),Charset.forName(encod)));
				read(inBuff, h);
				fl.add(f.toString().toLowerCase());
        	}
		} else if (f.toString().toLowerCase().endsWith(".zip"))
		{
			//System.out.println("file "+ f.getFileName());			
			Unzip(f.toString(), h, fl);
		}	
	}
	
	private static void read(BufferedReader in, Map<Object,Integer> h)
	{
	         try {
	             String line = in.readLine();
	             while(line!=null){
	                  StringTokenizer st = new StringTokenizer(line, delim);
	                  while(st.hasMoreTokens())
	                   	  add(h, st.nextToken());
	                  
	                  line = in.readLine();
	             }
	             //System.out.println("file "+ f.getFileName().toString() +": inserite "+h.size() +" parole");
	             in.close();
	         } catch(IOException e) {
	        	 e.printStackTrace();
	        }
	}
	
	private static void add(Map<Object,Integer> m, Object v)
	{
		/*
        Integer o = m.get(v);
        if(o==null)
        	m.put(v,1);
        else 
        	m.put(v,o+1);*/
        
        Integer o1 = m.putIfAbsent(v,1);
        if (o1 != null)
        	m.put(v, o1+1);
    }
	
	private static void Unzip (String zipFile, Map<Object,Integer> h, HashSet<String> fl) throws IOException {
		zipCount++;
		ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
        ZipEntry zipEntry = zis.getNextEntry();
        while (zipEntry != null) {
        	String st = new File(zipEntry.getName()).getName().toLowerCase();        	
        	
        	if (st.endsWith(".txt") && !fl.contains(st))
    		{
	        	BufferedReader in = new BufferedReader(new InputStreamReader(zis, "UTF-8"));
	        	String encod = checkTxt(in, LANG);
	        	if (!encod.isEmpty())
	        	{	        	
	        		BufferedReader inBuff = new BufferedReader(new InputStreamReader(zis, encod));
		        	read(inBuff, h);		        	
		        	fl.add(st);
	        	}
    		}        	
        	zipEntry = zis.getNextEntry();
        }
        zis.closeEntry();
        zis.close();
	}
	
	private static String checkTxt (BufferedReader in, String Lang) throws IOException
	{
		String enc = "";
		String lang = "";
		String line = in.readLine();
		
        while(line!=null)
        {
             if (line.startsWith("Language: "))
             {
            	 lang = line.substring(10);
            	 if (lang.compareToIgnoreCase(Lang) != 0 )
            		 return "";
            	else if (enc != "")
            		return enc;
            	 
             } else if (line.startsWith("Character set encoding: "))
             {
            	 enc = line.substring(24);
            	 if ((enc.compareToIgnoreCase("ISO Latin-1")==0) || (enc.compareToIgnoreCase("ISO 8859-1")==0))
            		 enc = "ISO-8859-1";
            	 else if (enc.compareToIgnoreCase("ISO-646-US (US-ASCII)") == 0)
            		 enc = "US-ASCII";
            	 else if (enc.compareToIgnoreCase("MP3") == 0)
            		 enc = "US-ASCII";
            	 else if (enc.compareToIgnoreCase("Unicode UTF-8") == 0)
            		 enc = "UTF-8";
            	 else if (enc.compareToIgnoreCase("IDO-8859-1") == 0)
            		 enc = "ISO-8859-1";
            	 else if (enc.compareToIgnoreCase("UTF?8") == 0)
            		 enc = "UTF-8";
            	 
            	 try {
            		 Charset inputCharset = Charset.forName(enc);
            	 } catch(Exception e) {
            		 System.out.println("ignored encoding " + enc);
            		 return "";
            	 }
            	 
            	 // UTF?8
            	 // ISO-8858-1
            	 // IDO-8859-1
            	 // US-ASCII, MIDI, Lilypond, MP3 and TeX
            	 
            	 if (lang != "")
            		 return enc;
             }
             
             line = in.readLine();
        }
		
		return "";
	}
}

