package edu.indiana.sice.idsl.jena;

import java.io.*; // BufferedReader, File, FileOutputStream, FileReader, IOException
import java.util.*; // Properties

import org.apache.commons.cli.*; // CommandLine, CommandLineParser, HelpFormatter, OptionBuilder, Options, ParseException, PosixParser
import org.apache.commons.cli.Option.*; // Builder

import org.apache.logging.log4j.*; // Logger, LogManager
import org.apache.logging.log4j.util.*; // PropertyFilePropertySource

import org.apache.jena.Jena;
import org.apache.jena.riot.*; //RDFFormat, RDFDataMgr
import org.apache.jena.query.*; //QueryExecution, QueryExecutionFactory, ResultSet, ResultSetFormatter

/**	Based on code from Matt Gianni, YarcData (April 2014).

	@author Jeremy Yang
*/
public class sparql_query
{
  static Logger logger = LogManager.getLogger(sparql_query.class);

  /////////////////////////////////////////////////////////////////////////////
  public static void RunQueries(String url, String idir, String ofmt, OutputStream ostream,int verbose)
      throws IOException
  {
    File d = new File(idir);
    for (File f: d.listFiles())
    {
      if ( f.getPath().endsWith(".rq") ) // only process .rq files
      {
        if (verbose>1) System.err.println("query file: "+f.getPath());
        String rq = ReadFile(f.getPath());
        QueryExecution qex = RunQuery(url, rq, verbose);
        OutputResults(qex, ostream, ofmt, verbose);
      }
      else
      {
        if (verbose>1) System.err.println("File skipped: "+f.getPath());
      }
    }      
  }

  /////////////////////////////////////////////////////////////////////////////
  public static QueryExecution RunQuery(String url, String rq, int verbose)
      throws IOException
  {
    if (verbose>1) logger.info("endpoint: "+url);
    if (verbose>1) System.err.println("endpoint: "+url);
    if (verbose>2) System.err.println("query: "+rq);
    QueryExecution qex = QueryExecutionFactory.sparqlService(url, rq);
    return qex;
  }

  /////////////////////////////////////////////////////////////////////////////
  public static void OutputResults(QueryExecution qex, OutputStream ostream, String ofmt, int verbose) throws IOException
  {
    long t0 = System.nanoTime();
    ResultSet result = qex.execSelect();
    
    try {
      if (ofmt.equalsIgnoreCase("XML"))
      {
        ResultSetFormatter.outputAsXML(ostream, result);
      }
      else if (ofmt.equalsIgnoreCase("JSON"))
      {
        ResultSetFormatter.outputAsJSON(ostream, result);
      }
      else if (ofmt.equalsIgnoreCase("TSV"))
      {
        ResultSetFormatter.outputAsTSV(ostream, result);
      }
      else
      {
        ResultSetFormatter.outputAsXML(ostream, result);
      }
    } catch (Exception e) {
      System.err.println(e.toString());
    }
    System.err.println(String.format("elapsed: %.3fs", (System.nanoTime()-t0)/1e9));
    qex.close();
  }

  /////////////////////////////////////////////////////////////////////////////
  private static String ReadFile(String ifile) throws IOException
  {
    BufferedReader reader = new BufferedReader(new FileReader(ifile));
    String line = null;
    StringBuilder sb = new StringBuilder();
    String ls = System.getProperty("line.separator");

    while ((line=reader.readLine())!=null)
      sb.append(line+ls);
    reader.close();
    return sb.toString();
  }

  /////////////////////////////////////////////////////////////////////////////
  private static String APPNAME="SPARQL_QUERY";
  private static int verbose=0;
  private static String url=null;
  private static String ifile=null;
  private static String rq=null;
  private static String idir=null;
  private static String ofile=null;
  private static String ofmt="TTL";

  /////////////////////////////////////////////////////////////////////////////
  public static void main(String[] args) throws Exception
  {
    String HELPHEADER =  "SPARQL_APP: Query Sparql endpoint.";
    String HELPFOOTER = ("Jena version: "+Jena.VERSION+" ("+Jena.BUILD_DATE+")\n"
      +"OFMTS: RDF|TTL|NT|NTRIPLES|JSON|TSV\n");
    Options opts = new Options();
    opts.addOption(Option.builder("url").longOpt("endpoint_url").hasArg().argName("URL").desc("endpoint URL").required(true).build());
    opts.addOption(Option.builder("i").longOpt("ifile_sparql").hasArg().argName("IFILE_SPARQL").desc("Input Sparql file").build());
    opts.addOption(Option.builder("rq").longOpt("sparql").hasArg().argName("SPARQL").desc("Sparql").build());

    opts.addOption(Option.builder("o").longOpt("ofile").hasArg().argName("OFILE").desc("Output file").build());
    opts.addOption(Option.builder("idir").hasArg().argName("IDIR").desc("Input directory").build());
    opts.addOption(Option.builder("ofmt").hasArg().argName("OFMT").desc("Output format").build());
    opts.addOption("v", "verbose", false, "Verbose.");
    opts.addOption("vv", "vverbose", false, "Very verbose.");
    opts.addOption("vvv", "vvverbose", false, "Very very verbose.");
    opts.addOption("h", "help", false, "Show this help.");
    HelpFormatter helper = new HelpFormatter();
    CommandLineParser clip = new PosixParser();
    CommandLine clic = null;
    try {
      clic = clip.parse(opts, args);
    } catch (ParseException e) {
      helper.printHelp(APPNAME, HELPHEADER, opts, e.getMessage(), true);
      System.exit(0);
    }
    if (clic.hasOption("ifile_sparql")) ifile = clic.getOptionValue("ifile_sparql");
    if (clic.hasOption("sparql")) rq = clic.getOptionValue("sparql");
    if (clic.hasOption("o")) ofile = clic.getOptionValue("o");
    if (clic.hasOption("vvv")) verbose = 3;
    else if (clic.hasOption("vv")) verbose = 2;
    else if (clic.hasOption("v")) verbose = 1;
    if (clic.hasOption("h")) {
      helper.printHelp(APPNAME, HELPHEADER, opts, HELPFOOTER, true);
      System.exit(0);
    }

    Properties props = new Properties();
    props.load(new FileInputStream(System.getProperty("user.home")+"/.log4j/properties/log4j.properties")); 

    if (rq==null && ifile==null && idir==null) helper.printHelp(APPNAME, HELPHEADER, opts,"--sparql or --ifile_sparql or -idir required", true);

    OutputStream ostream = System.out;
    if (ofile!=null)
    {
      try { ostream = new FileOutputStream(ofile); }
      catch (Exception e) { helper.printHelp(APPNAME, HELPHEADER, opts, e.getMessage(), true); }
    }

    if (idir!=null)
    {
      if (verbose>0) System.err.println("idir: "+idir);
      RunQueries(url, idir, ofmt, ostream, verbose);
    }
    else if (rq!=null)
    {
      if (verbose>0) System.err.println("ifile: "+ifile);
      QueryExecution qex = RunQuery(url, rq, verbose);
      OutputResults(qex, ostream, ofmt, verbose);
    }
    else
    {
      if (verbose>0) System.err.println("ifile: "+ifile);
      rq = ReadFile(ifile);
      QueryExecution qex = RunQuery(url, rq, verbose);
      OutputResults(qex, ostream, ofmt, verbose);
    }

    if (ofile!=null)
    {
      try { ostream.close(); } catch (Exception e) { System.err.println(e.getMessage()); }
    }
  }
}
