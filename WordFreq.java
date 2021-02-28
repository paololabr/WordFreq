
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class WordFreq {

	final static String DELIM = " ,.;?!\"\'";
	final static String LANG = "English";
	final static String DEFAULT_ENCODING = "UTF-8";
	final static String DEFAULT_DIR = "./pgdvd042010";

	public static void main(String[] args) throws IOException {
		String path = DEFAULT_DIR;
		if (args.length > 0 && !args[0].trim().isEmpty())
			path = args[0];

		File f = new File(path);
		if (!f.isDirectory()) {
			System.out.println("Folder not found: " + path);
			return;
		}

		AtomicLong zipCount = new AtomicLong();
		long tokensCount = 0;

		Map<Object, Integer> h = new HashMap<Object, Integer>(); // token, freq.
		Map<Object, Integer> t = new TreeMap<Object, Integer>(); // freq., word
		HashSet<String> txtList = new HashSet<String>(); // txt file list

		Files.walk(Paths.get(path)).filter(Files::isReadable).filter(Files::isRegularFile)
		.filter(file -> (file.toString().toLowerCase().endsWith(".txt")
				|| file.toString().toLowerCase().endsWith(".zip")))
		.forEach(file -> {
			try {
				ProcessFile(file, h, txtList, zipCount);
			} catch (IOException e) {
				e.printStackTrace();
			}
		});

		for (Integer v : h.values()) {
			add(t, v);
			tokensCount += v;
		}

		FileWriter fw = new FileWriter("result.txt");

		fw.write("Unique Txt files (" + LANG + "):\t" + txtList.size() + "\n");
		fw.write("Zip files:\t" + zipCount.get() + "\n");
		fw.write("Tokens:\t" + tokensCount + "\n");
		fw.write("Words:\t" + h.size() + "\n\n");

		fw.write("Words\t\tFreq\n\n");
		for (Object o : t.keySet())
			fw.write(t.get(o) + "\t\t" + o.toString() + "\n");

		fw.close();

		FileWriter fl = new FileWriter("fileList.txt");
		for (String txt : txtList)
			fl.write(txt + "\n");
		fl.close();

	}

	private static void ProcessFile(Path f, Map<Object, Integer> h, HashSet<String> fl, AtomicLong zipCount)
			throws IOException {
		if (f.toString().toLowerCase().endsWith(".txt") && !fl.contains(f.getFileName().toString().toLowerCase())) {
			BufferedReader in = new BufferedReader(new FileReader(f.toString(), Charset.forName(DEFAULT_ENCODING)));
			String encod = checkTxt(in, LANG);
			if (!encod.isEmpty()) {
				BufferedReader inBuff = new BufferedReader(new FileReader(f.toString(), Charset.forName(encod)));
				read(inBuff, h);
				fl.add(f.getFileName().toString().toLowerCase());
			}
		} else if (f.toString().toLowerCase().endsWith(".zip")) {
			Unzip(f.toString(), h, fl);
			zipCount.incrementAndGet();
		}
	}

	private static void read(BufferedReader in, Map<Object, Integer> h) {
		try {
			boolean startFound = false;
			String line = in.readLine();
			while (line != null) {
				if (line.startsWith("***END") || line.startsWith("*** END"))
					break;

				if (startFound) {
					StringTokenizer st = new StringTokenizer(line, DELIM);
					while (st.hasMoreTokens())
						add(h, st.nextToken());
				} else if (line.startsWith("***START") || line.startsWith("*** START"))
					startFound = true;

				line = in.readLine();
			}

			in.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void add(Map<Object, Integer> m, Object v) {
		Integer o = m.putIfAbsent(v, 1);
		if (o != null)
			m.put(v, o + 1);
	}

	private static void Unzip(String zipFile, Map<Object, Integer> h, HashSet<String> fl) throws IOException {
		ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
		ZipEntry zipEntry = zis.getNextEntry();
		while (zipEntry != null) {
			String st = new File(zipEntry.getName()).getName().toLowerCase();

			if (st.endsWith(".txt") && !fl.contains(st)) {
				BufferedReader in = new BufferedReader(new InputStreamReader(zis, DEFAULT_ENCODING));
				String encod = checkTxt(in, LANG);
				if (!encod.isEmpty()) {
					ZipFile zip = new ZipFile(zipFile);
					ZipEntry entry = zip.getEntry(zipEntry.getName());

					BufferedReader inBuff = new BufferedReader(new InputStreamReader(zip.getInputStream(entry), encod));
					read(inBuff, h);
					fl.add(st);
					zip.close();
				}
			}
			zipEntry = zis.getNextEntry();
		}
		zis.closeEntry();
		zis.close();
	}

	private static String checkTxt(BufferedReader in, String Lang) throws IOException {
		String enc = "";
		String lang = "";
		String line = in.readLine();

		while (line != null) {
			if (line.startsWith("Language: ")) {
				lang = line.substring(10);
				if (lang.compareToIgnoreCase(Lang) != 0)
					return "";
				else if (enc != "")
					return enc;

			} else if (line.startsWith("Character set encoding: ")) {
				String str = line.substring(24).trim();
				byte[] ba = str.getBytes(StandardCharsets.US_ASCII);
				enc = new String(ba, StandardCharsets.US_ASCII);

				if ((enc.compareToIgnoreCase("ISO Latin-1") == 0) || (enc.compareToIgnoreCase("ISO 8859-1") == 0))
					enc = "ISO-8859-1";
				else if (enc.compareToIgnoreCase("ISO-646-US (US-ASCII)") == 0)
					enc = "US-ASCII";
				else if (enc.compareToIgnoreCase("MP3") == 0)
					enc = "US-ASCII";
				else if (enc.compareToIgnoreCase("Unicode UTF-8") == 0)
					enc = "UTF-8";
				else if (enc.compareToIgnoreCase("IDO-8859-1") == 0) // scritto male
					enc = "ISO-8859-1";
				else if (enc.compareToIgnoreCase("ISO-8858-1") == 0) // non esiste ISO-8858-1 forse il ISO 8859-1 ?
					enc = "ISO-8859-1";

				try {
					Charset inputCharset = Charset.forName(enc);
				} catch (Exception e) {
					System.out.println("ignored encoding " + enc + " assigned to " + DEFAULT_ENCODING);
					enc = DEFAULT_ENCODING;
					if (lang != "")
						return enc;
				}

				// US-ASCII, MIDI, Lilypond, MP3 and TeX

				if (lang != "")
					return enc;
			}

			line = in.readLine();
		}

		return "";
	}
}
