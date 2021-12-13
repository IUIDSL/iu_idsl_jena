package edu.indiana.sice.idsl.jena;

import java.io.*; // BufferedReader, File, FileOutputStream, FileReader, IOException
import java.util.*;
import jena.version;

import org.apache.log4j.*; // Logger, BasicConfigurator
import org.apache.jena.riot.*; //RDFFormat, RDFDataMgr

import com.hp.hpl.jena.query.*; //QueryExecution, QueryExecutionFactory, ResultSet, ResultSetFormatter
import com.hp.hpl.jena.sparql.resultset.*; //RDFOutput

/**	Based on code from Matt Gianni, YarcData (April 2014).

	@author Jeremy Yang
*/
public class sparql_query
{
  static Logger logger = Logger.getLogger(sparql_query.class);

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
  public static void OutputResults(QueryExecution qex, OutputStream ostream, String ofmt, int verbose)
      throws IOException
  {
    long t0 = System.nanoTime();
    ResultSet result = qex.execSelect();
    
    try {
      if (ofmt.equalsIgnoreCase("RDF"))
      {
        ResultSetFormatter.outputAsRDF(ostream, RDFFormat.PLAIN.toString(), result); //Fails.
        //RDFOutput.outputAsRDF(ostream, RDFFormat.PLAIN.toString(), result); //Fails.
      }
      else if (ofmt.equalsIgnoreCase("TTL"))
      {
        ResultSetFormatter.outputAsRDF(ostream, RDFFormat.TTL.toString(), result); //Fails.
      }
      else if (ofmt.equalsIgnoreCase("TURTLE"))
      {
        ResultSetFormatter.outputAsRDF(ostream, RDFFormat.TURTLE.toString(), result); //Fails.
      }
      else if (ofmt.equalsIgnoreCase("NTRIPLES"))
      {
        ResultSetFormatter.outputAsRDF(ostream, RDFFormat.NTRIPLES.toString(), result); //Fails.
      }
      else if (ofmt.equalsIgnoreCase("NT"))
      {
        ResultSetFormatter.outputAsRDF(ostream, RDFFormat.NT.toString(), result); //Fails.
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
        ResultSetFormatter.outputAsRDF(ostream, RDFFormat.TTL.toString(), result); //Fails.
      }
    } catch (Exception e) {
      System.err.println(e.toString());
    }
    System.err.println(String.format("elapsed: %.3fs",(System.nanoTime()-t0)/1e9));
    
    qex.close();
  }

  /////////////////////////////////////////////////////////////////////////////
  private static String ReadFile(String ifile)
	throws IOException
  {
    BufferedReader reader = new BufferedReader(new FileReader(ifile));
    String         line = null;
    StringBuilder  sb = new StringBuilder();
    String         ls = System.getProperty("line.separator");

    while ((line=reader.readLine())!=null)
      sb.append(line+ls);
    reader.close();
    return sb.toString();
  }

  /////////////////////////////////////////////////////////////////////////////
  private static int verbose=0;
  private static String url=null;
  private static String ifile=null;
  private static String rq=null;
  private static String idir=null;
  private static String ofile=null;
  private static String ofmt="TTL";
  private static void Help(String msg)
  {
    System.err.println(msg+"\n"
      +"sparql_query - query Sparql endpoint\n"
      +"\n"
      +"usage: sparql_query [options]\n"
      +"  required:\n"
      +"    -url URL .................... endpoint\n"
      +"   and\n"
      +"    -rq SPARQL .................. input query\n"
      +"   and\n"
      +"    -i IFILE .................... input query .rq file\n"
      +"   or\n"
      +"    -idir IDIR .................. input dir, containing query .rq files\n"
      +"  options:\n"
      +"    -o OFILE .................... output file\n"
      +"    -ofmt OFMT .................. output format ["+ofmt+"]\n"
      +"    -v[v[v]] .................... verbose [very [very]]\n"
      +"    -h .......................... this help\n"
      +"\n"
      +"OFMTS: RDF|TTL|NT|NTRIPLES|JSON|TSV\n"
      +"\n"
      +"Jena version: "+jena.version.VERSION+" ("+jena.version.BUILD_DATE+")\n"
	);
    System.exit(1);
  }

  /////////////////////////////////////////////////////////////////////////////
  private static void ParseCommand(String args[])
  {
    for (int i=0;i<args.length;++i)
    {
      if (args[i].equals("-url")) url=args[++i];
      else if (args[i].equals("-i")) ifile=args[++i];
      else if (args[i].equals("-rq")) rq=args[++i];
      else if (args[i].equals("-idir")) idir=args[++i];
      else if (args[i].equals("-o")) ofile=args[++i];
      else if (args[i].equals("-ofmt")) ofmt=args[++i];
      else if (args[i].equals("-v")) verbose=1;
      else if (args[i].equals("-vv")) verbose=2;
      else if (args[i].equals("-vvv"))verbose=3;
      else if (args[i].equals("-h")) Help("");
      else Help("Unknown option: "+args[i]);
    }
  }

  /////////////////////////////////////////////////////////////////////////////
  public static void main(String[] args)
	throws Exception
  {
    //Relative or absolute path for JVM:
    PropertyConfigurator.configure(System.getProperty("user.home")+"/src/iu-jejyang/properties/log4j.properties"); //log4j to stderr

    ParseCommand(args);
    //url = "http://cheminfov.informatics.indiana.edu:8890/sparql";
    if (url==null) Help("-url required");
    if (rq==null && ifile==null && idir==null) Help("-rq or -i or -idir required");

    OutputStream ostream = System.out;
    if (ofile!=null)
    {
      try { ostream = new FileOutputStream(ofile); }
      catch (Exception e) { Help(e.getMessage()); }
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
