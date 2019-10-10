package edu.indiana.sice.idsl.jena;

import java.io.*;
import java.nio.file.*; //Files, Path
import java.util.*;
import java.util.regex.*;
import java.net.*; //URL

import com.hp.hpl.jena.ontology.*;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.util.iterator.*; //ExtendedIterator
import com.hp.hpl.jena.vocabulary.*;
import com.hp.hpl.jena.reasoner.*; //Reasoner, ReasonerRegistry, InfModel
import com.hp.hpl.jena.reasoner.rulesys.*; //GenericRuleReasonerFactory,GenericRuleReasoner
import com.hp.hpl.jena.query.* ; //ARQ, Dataset, DatasetFactory
import com.hp.hpl.jena.sparql.*; //Sparql
import com.hp.hpl.jena.sparql.engine.http.*; //QueryEngineHTTP
import com.hp.hpl.jena.sparql.core.*; //Prologue

import com.fasterxml.jackson.core.*; //JsonFactory, JsonGenerator
import com.fasterxml.jackson.databind.*; //ObjectMapper, JsonNode

import org.apache.jena.atlas.logging.LogCtl;

/**	Static utility methods for Jena.
	@author Jeremy Yang
*/
public class jena_utils
{
  /////////////////////////////////////////////////////////////////////////////
  /**	First file should be default graph.
  */
  public static void LoadRDF(String [] ifiles_rdf, Model [] rmods, Dataset dset,int verbose)
  {
    int i_mod=0;
    for (String ifile_rdf: ifiles_rdf)
    {
      System.err.println("ifile_rdf: "+ifile_rdf+((i_mod==0)?" (DEFAULT GRAPH)":""));
      rmods[i_mod] = ModelFactory.createDefaultModel();
      String fext = ifile_rdf.substring(1+ifile_rdf.lastIndexOf('.'));
      String dlang = (fext.equalsIgnoreCase("TTL")?"TTL":(fext.equalsIgnoreCase("N3")?"N3":"RDF/XML"));
      System.err.println("dlang: "+dlang);
      try {
        InputStream instr = FileManager.get().open(ifile_rdf);
        rmods[i_mod].read(instr,"",dlang); //arg2=base_uri, arg3=lang
      }
      catch (Exception e) { System.err.println("ERROR: "+e.getMessage()); }

      ++i_mod;
    }
    for (i_mod=0;i_mod<rmods.length;++i_mod)
      dset.addNamedModel(ifiles_rdf[i_mod],rmods[i_mod]);
    dset.setDefaultModel(rmods[0]);
  }

  /////////////////////////////////////////////////////////////////////////////
  public static void DescribeDataset(Dataset dset,int verbose)
  {
    Iterator<String> name_itr = dset.listNames();
    while (name_itr.hasNext())
    {
      String name = name_itr.next();
      Model nmod = dset.getNamedModel(name);
      System.err.println("\tnamed graph: "+name+"; size: "+((nmod!=null)?Long.toString(nmod.size()):"null (ERROR)"));
    }
    System.err.println("\tdefault graph size: "+dset.getDefaultModel().size());
  }

  /////////////////////////////////////////////////////////////////////////////
  public static OntModel LoadOntologyFile(String ifile_ont,String otype,int verbose)
  {
    System.err.println("ifile_ont: "+ifile_ont);
    OntDocumentManager odocmgr = new OntDocumentManager();

    OntModelSpec omodspec = new OntModelSpec(otype.equalsIgnoreCase("RDFS") ? OntModelSpec.RDFS_MEM : OntModelSpec.OWL_MEM);
    omodspec.setDocumentManager(odocmgr);
    OntModel omod = ModelFactory.createOntologyModel(omodspec, null);
    try {
      InputStream instr = FileManager.get().open(ifile_ont);
      omod.read(instr, ""); //arg2=base_uri
    }
    catch (Exception e) { System.err.println("ERROR: "+e.getMessage()); }
    return omod;
  }

  /////////////////////////////////////////////////////////////////////////////
  public static OntModel LoadOntologyUrl(String url_ont,String otype,int verbose)
  {
    System.err.println("url_ont: "+url_ont);
    OntDocumentManager odocmgr = new OntDocumentManager();

    OntModelSpec omodspec = new OntModelSpec(otype.equalsIgnoreCase("RDFS") ? OntModelSpec.RDFS_MEM : OntModelSpec.OWL_MEM);
    omodspec.setDocumentManager(odocmgr);
    OntModel omod = ModelFactory.createOntologyModel(omodspec, null);
    try {
      //URL url = new URL(url_ont);
      //InputStream instr = url.openStream();
      //omod.read(instr, ""); //arg2=base_uri

      omod.read(url_ont, "", null); //arg2=base_uri,arg3=lang, null means default RDF/XML
    }
    catch (Exception e) { System.err.println("ERROR: "+e.getMessage()); }
    return omod;
  }

  /////////////////////////////////////////////////////////////////////////////
  public static void DescribeRDF(Model rmod,int verbose)
  {
    System.err.println("\tmodel size: "+rmod.size());

    ResIterator res_itr = rmod.listSubjects(); //subjects (unique list)
    int i_subj=0;
    while (res_itr.hasNext())
    {
      Resource res = res_itr.nextResource();
      ++i_subj;
    }
    System.err.println("\tsubject count: "+i_subj);

    NodeIterator node_itr = rmod.listObjects();
    int i_obj=0;
    while (node_itr.hasNext())
    {
      RDFNode node = node_itr.nextNode();
      ++i_obj;
    }
    System.err.println("\tobject count: "+i_obj);

    //StmtIterator stmt_itr = rmod.listStatements(Resource s, Property p, RDFNode o); //query
    //StmtIterator stmt_itr = rmod.listStatements(Selector s); //query

    StmtIterator stmt_itr = rmod.listStatements();
    int i_stmt=0;
    while (stmt_itr.hasNext())
    {
      ++i_stmt;
      Statement stmt = stmt_itr.nextStatement();
      Resource subj = stmt.getSubject();
      Property prop = stmt.getPredicate();
      RDFNode obj = stmt.getObject();
      if (verbose>1)
        System.err.println(subj.toString()+"\t"+prop.toString()+"\t"+obj.toString()+" ("+stmt.getLanguage()+")");
    }
    System.err.println("\tstatement count: "+i_stmt);
    if (verbose>2) rmod.write(System.err);
  }

  /////////////////////////////////////////////////////////////////////////////
  public static void DescribeOntology(OntModel omod,int verbose)
  {
    System.err.println("model size: "+omod.size());
    System.err.println("imported:");
    for (String uri: omod.listImportedOntologyURIs())
      System.err.println("\t"+uri);
    ExtendedIterator<OntClass> cls_itr = omod.listNamedClasses();
    int i_cls=0;
    while (cls_itr.hasNext())
    {
      ++i_cls;
      OntClass cls = cls_itr.next();
      if (verbose>1)
        System.err.println("\t"+cls.getURI());
    }
    System.err.println("class count: "+i_cls);
    if (verbose>2) omod.write(System.err);  
  }

  /////////////////////////////////////////////////////////////////////////////
  public static void OntModelClassList(OntModel omod,PrintWriter fout_writer,int verbose)
  {
    ExtendedIterator<OntClass> cls_itr = omod.listNamedClasses();
    int i_cls=0;
    while (cls_itr.hasNext())
    {
      ++i_cls;
      OntClass cls = cls_itr.next();
      String uri=cls.getURI();
      String label=cls.getLabel(null);
      String comment=cls.getComment(null);
      label=(label!=null)?label.replaceFirst("[\\s]+$",""):"";
      fout_writer.write(String.format("<%s>\trdfs:label\t\"%s\" .\n",uri,label));
      fout_writer.flush();
      if (verbose>1)
      {
        System.err.println(String.format("<%s>",uri));
        System.err.println(String.format("\trdfs:label\t\"%s\" ;",label));
        System.err.println(String.format("\rdfs:tcomment\t\"%s\" .",comment));
      }
    }
    System.err.println("classes: "+i_cls);
  }

  /////////////////////////////////////////////////////////////////////////////
  public static void OntModelRootclassList(OntModel omod,PrintWriter fout_writer,int verbose)
  {
    ExtendedIterator<OntClass> cls_itr = omod.listNamedClasses();
    int i_cls=0;
    while (cls_itr.hasNext())
    {
      ++i_cls;
      OntClass cls = cls_itr.next();
      String uri=cls.getURI();
      String label=cls.getLabel(null);
      label=(label!=null)?label.replaceFirst("[\\s]+$",""):"";
      if (cls.isHierarchyRoot())
        fout_writer.write(String.format("<%s>\trdfs:label\t\"%s\" .\n",uri,label));
      fout_writer.flush();
    }
    System.err.println("classes: "+i_cls);
  }

  /////////////////////////////////////////////////////////////////////////////
  public static void OntModelSubclassList(OntModel omod, PrintWriter fout_writer, int verbose)
  {
    ExtendedIterator<OntClass> cls_itr = omod.listNamedClasses();
    int i_cls=0;
    int i_subcls=0;
    while (cls_itr.hasNext())
    {
      ++i_cls;
      OntClass cls = cls_itr.next();
      String uri=cls.getURI();
      String label=cls.getLabel(null);
      String comment=cls.getComment(null);
      label=(label!=null)?label.replaceFirst("[\\s]+$",""):"";

      ExtendedIterator<OntClass> subcls_itr = cls.listSubClasses();
      while (subcls_itr.hasNext())
      {
        ++i_subcls;
        OntClass subcls = subcls_itr.next();
        String uri_sub=subcls.getURI();
        String label_sub=subcls.getLabel(null);
        String comment_sub=subcls.getComment(null);
        label_sub=(label_sub!=null)?label_sub.replaceFirst("[\\s]+$",""):"";

        fout_writer.write(String.format("<%s>\trdfs:subClassOf\t<%s> .\n",uri_sub,uri));
        if (verbose>1)
          System.err.println(String.format("%s (label:%s)\trdfs:subClassOf\t%s (label:%s)",uri_sub,label_sub,uri,label));
      }
    }
    System.err.println("classes: "+i_cls+", subclasses: "+i_subcls);
  }

  /////////////////////////////////////////////////////////////////////////////
  /**	Converts ontology class hierarchy to TSV.
  */
  public static void OntModel2TSV(OntModel omod, PrintWriter fout_writer, int verbose)
        throws Exception
  {
    fout_writer.write("node_or_edge\tid\tlabel\tcomment\tsource\ttarget\turi\n");
    ExtendedIterator<OntClass> cls_itr = omod.listNamedClasses();
    int i_cls=0;
    int i_subcls=0;
    while (cls_itr.hasNext())
    {
      OntClass cls = cls_itr.next();
      String uri=cls.getURI();
      if (uri==null) continue; //error
      String id=uri.replaceFirst("^.*/","");
      String label=cls.getLabel(null);
      label=(label!=null)?label.replaceFirst("[\\s]+$",""):"";
      label=(label!=null)?label.replaceAll("&","&amp;"):"";
      label=(label!=null)?label.replaceAll("<","&lt;"):"";
      label=(label!=null)?label.replaceAll(">","&gt;"):"";
      label=(label!=null)?label.replaceAll("[\n\r]"," "):"";
      String comment=cls.getComment(null);
      comment=(comment!=null)?comment.replaceAll("&","&amp;"):"";
      comment=(comment!=null)?comment.replaceAll("<","&lt;"):"";
      comment=(comment!=null)?comment.replaceAll(">","&gt;"):"";
      comment=(comment!=null)?comment.replaceAll("[\n\r]"," "):"";
      fout_writer.write(String.format("node\t%s\t%s\t%s\t\t\t%s\n", id, label, comment, uri)); 
      ++i_cls;
    }
    cls_itr = omod.listNamedClasses(); //rewind for subclasses/edges
    while (cls_itr.hasNext())
    {
      ++i_cls;
      OntClass cls = cls_itr.next();
      String uri=cls.getURI();
      ExtendedIterator<OntClass> subcls_itr = cls.listSubClasses();
      while (subcls_itr.hasNext())
      {
        OntClass subcls = subcls_itr.next();
        String uri_sub=subcls.getURI(); //target
        if (uri_sub==null) continue; //error
        String label="has_subclass";
        fout_writer.write(String.format("edge\t\t%s\t\t%s\t%s\t\n", label, uri, uri_sub)); 
        ++i_subcls;
      }
    }
    System.err.println("nodes (classes): "+i_cls+", edges (subclasses): "+i_subcls);
  }

  /////////////////////////////////////////////////////////////////////////////
  /**	Converts ontology class hierarchy to a Cytoscape JS format directed
	graph for processing and viewing.  Use Jackson-databind library.
  */
  public static void OntModel2CYJS(OntModel omod, PrintWriter fout_writer, int verbose)
        throws Exception
  {
    ExtendedIterator<OntClass> cls_itr = omod.listNamedClasses();
    int i_cls=0;
    int i_subcls=0;

    HashMap<String,Object> root = new HashMap<String,Object>();
    root.put("format_version", "1.0");
    root.put("generated_by", "cytoscape-3.7.1");
    root.put("target_cytoscapejs_version", "~2.1");
    HashMap<String,Object> data = new HashMap<String,Object>();
    data.put("shared_name", "ONTOLOGY_CLASS_HIERARCHY");
    data.put("name", "ONTOLOGY_CLASS_HIERARCHY");
    data.put("SUID", new Integer(52)); //integer
    data.put("selected", new Boolean(true)); //boolean
    root.put("data", data);

    ArrayList<HashMap<String, Object> > nodes = new ArrayList<HashMap<String, Object> >();
    while (cls_itr.hasNext())
    {
      OntClass cls = cls_itr.next();
      String uri=cls.getURI();
      if (uri==null) continue; //error
      String id=uri.replaceFirst("^.*/","");
      String label=cls.getLabel(null);
      label=(label!=null)?label.replaceFirst("[\\s]+$",""):"";
      label=(label!=null)?label.replaceAll("&","&amp;"):"";
      label=(label!=null)?label.replaceAll("<","&lt;"):"";
      label=(label!=null)?label.replaceAll(">","&gt;"):"";
      String comment=cls.getComment(null);
      comment=(comment!=null)?comment.replaceAll("&","&amp;"):"";
      comment=(comment!=null)?comment.replaceAll("<","&lt;"):"";
      comment=(comment!=null)?comment.replaceAll(">","&gt;"):"";
      HashMap<String, Object> node = new HashMap<String, Object>();
      HashMap<String, Object> nodedata = new HashMap<String, Object>();
      nodedata.put("id", id);
      nodedata.put("name", uri);
      nodedata.put("label", label);
      nodedata.put("comment", comment);
      node.put("data", nodedata);
      nodes.add(node);

      ++i_cls;
    }

    HashMap<String, Object> elements = new HashMap<String, Object>();
    elements.put("nodes", nodes);

    ArrayList<HashMap<String, Object> > edges = new ArrayList<HashMap<String, Object> >();
    cls_itr = omod.listNamedClasses(); //rewind for subclasses/edges
    while (cls_itr.hasNext())
    {
      ++i_cls;
      OntClass cls = cls_itr.next();
      String uri=cls.getURI();
      String id=uri.replaceFirst("^.*/","");
      ExtendedIterator<OntClass> subcls_itr = cls.listSubClasses();
      while (subcls_itr.hasNext())
      {
        OntClass subcls = subcls_itr.next();
        String uri_sub=subcls.getURI();
        if (uri_sub==null) continue; //error
        String id_sub=uri_sub.replaceFirst("^.*/","");
        HashMap<String, Object> edge = new HashMap<String, Object>();
        HashMap<String, Object> edgedata = new HashMap<String, Object>();
        edgedata.put("source", id);
        edgedata.put("target", id_sub);
        edge.put("data", edgedata);
        edges.add(edge);
        ++i_subcls;
      }
    }
    System.err.println("nodes (classes): "+i_cls+", edges (subclasses): "+i_subcls);
    elements.put("edges", edges);
    root.put("elements", elements);

    ObjectMapper mapper = new ObjectMapper();
    //fout_writer.write(mapper.writeValueAsString(root)+"\n"); //ok but unpretty

    JsonFactory jsf = mapper.getFactory();
    JsonGenerator jsg = jsf.createGenerator(fout_writer);
    jsg.useDefaultPrettyPrinter();
    jsg.writeObject(root);
  }

  /////////////////////////////////////////////////////////////////////////////
  /**	Converts ontology class hierarchy to a GraphML format directed
	graph for processing and viewing.
  */
  public static void OntModel2GraphML(OntModel omod, PrintWriter fout_writer, int verbose)
  {
    ExtendedIterator<OntClass> cls_itr = omod.listNamedClasses();
    int i_cls=0;
    int i_subcls=0;
    fout_writer.write(
"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
+"<graphml xmlns=\"http://graphml.graphdrawing.org/xmlns\"\n"
+"         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
+"         xsi:schemaLocation=\"http://graphml.graphdrawing.org/xmlns\n"
+"         http://graphml.graphdrawing.org/xmlns/1.0/graphml.xsd\">\n"
+"  <key id=\"name\" for=\"graph\" attr.name=\"name\" attr.type=\"string\"/>\n"
+"  <key id=\"Author\" for=\"graph\" attr.name=\"Author\" attr.type=\"string\"/>\n"
+"  <key id=\"name\" for=\"node\" attr.name=\"name\" attr.type=\"string\"/>\n"
+"  <key id=\"uri\" for=\"node\" attr.name=\"uri\" attr.type=\"string\"/>\n"
+"  <key id=\"comment\" for=\"node\" attr.name=\"comment\" attr.type=\"string\"/>\n"
+"  <key id=\"type\" for=\"edge\" attr.name=\"type\" attr.type=\"string\"/>\n"
+"  <key id=\"weight\" for=\"edge\" attr.name=\"weight\" attr.type=\"double\"/>\n"
+"  <graph id=\"ONTOLOGY_CLASS_HIERARCHY\" edgedefault=\"directed\">\n"
+"    <data key=\"name\">Ontology class hierarchy</data>\n"
	);

    while (cls_itr.hasNext())
    {
      OntClass cls = cls_itr.next();
      String uri=cls.getURI();
      if (uri==null) continue; //error
      String id=uri.replaceFirst("^.*/","");
      String label=cls.getLabel(null);
      label=(label!=null)?label.replaceFirst("[\\s]+$",""):"";
      label=(label!=null)?label.replaceAll("&","&amp;"):"";
      label=(label!=null)?label.replaceAll("<","&lt;"):"";
      label=(label!=null)?label.replaceAll(">","&gt;"):"";
      String comment=cls.getComment(null);
      comment=(comment!=null)?comment.replaceAll("&","&amp;"):"";
      comment=(comment!=null)?comment.replaceAll("<","&lt;"):"";
      comment=(comment!=null)?comment.replaceAll(">","&gt;"):"";
      fout_writer.write(
"    <node id=\""+id+"\">\n"
+"      <data key=\"uri\">"+uri+"</data>\n"
+"      <data key=\"name\">"+label+"</data>\n"
+"      <data key=\"comment\">"+comment+"</data>\n"
+"    </node>\n"
	);
      ++i_cls;
    }

    cls_itr = omod.listNamedClasses(); //rewind for subclasses/edges
    while (cls_itr.hasNext())
    {
      ++i_cls;
      OntClass cls = cls_itr.next();
      String uri=cls.getURI();
      String id=uri.replaceFirst("^.*/","");
      ExtendedIterator<OntClass> subcls_itr = cls.listSubClasses();
      while (subcls_itr.hasNext())
      {
        OntClass subcls = subcls_itr.next();
        String uri_sub=subcls.getURI();
        if (uri_sub==null) continue; //error
        String id_sub=uri_sub.replaceFirst("^.*/","");
        fout_writer.write(
"    <edge source=\""+id+"\" target=\""+id_sub+"\">\n"
+"      <data key=\"type\">subclass</data>\n"
+"    </edge>\n"
	);
        ++i_subcls;

      }
    }
      fout_writer.write(
"  </graph>\n"
+"</graphml>\n"
	);
    System.err.println("nodes (classes): "+i_cls+", edges (subclasses): "+i_subcls);
  }

  /////////////////////////////////////////////////////////////////////////////
  public static void ValidateModel(Model mod, Reasoner reasoner)
  {
    if (reasoner==null)
    {
      reasoner = ReasonerRegistry.getOWLReasoner();
      reasoner = reasoner.bindSchema(mod);
    }
    InfModel infmod = ModelFactory.createInfModel(reasoner, mod);
    ValidityReport validity = infmod.validate();
    if (validity.isValid()) {
      System.err.println("OK, no conflicts.");
    }
    else
    {
      System.err.println("Conflicts:");
      for (Iterator i = validity.getReports(); i.hasNext(); ) {
        ValidityReport.Report report = (ValidityReport.Report)i.next();
        System.err.println(" - " + report);
      }
    }
  }

  /////////////////////////////////////////////////////////////////////////////
  public static void QueryRDF(Dataset dset, String rq, PrintWriter fout_writer, int verbose)
  {
    Query query = QueryFactory.create(rq);
    QueryExecution qe = QueryExecutionFactory.create(query, dset);
    ResultSet results = qe.execSelect();
    Prologue prol = query.getPrologue(); //to abbreviate IRIs
    fout_writer.write(ResultSetFormatter.asText(results,prol));
    if (verbose>2) ResultSetFormatter.out(System.err, results, query);
    qe.close();
  }

  /////////////////////////////////////////////////////////////////////////////
  public static void QueryEndpoint(String url, String rq, PrintWriter fout_writer, int verbose)
  {
    Query query = QueryFactory.create(rq);
    QueryEngineHTTP qe = new QueryEngineHTTP(url,query);
    ResultSet results = qe.execSelect();
    Prologue prol = query.getPrologue(); //to abbreviate IRIs
    fout_writer.write(ResultSetFormatter.asText(results,prol));
    if (verbose>2) ResultSetFormatter.out(System.err, results, query);
    qe.close();
  }

  /////////////////////////////////////////////////////////////////////////////
  public static void InferAllStatements(Model mod, Reasoner reasoner, PrintWriter fout_writer, int verbose)
  {
    InfModel infmod = ModelFactory.createInfModel(reasoner, mod);
    //Resource A = infmod.getResource("http://foo#bar"); //DEBUG
    Resource A = null ;
    //System.err.println("A * * =>");
    Iterator stmt_itr = infmod.listStatements((Resource)A, (Property)null, (RDFNode)null);
    int i_stmt=0;
    while (stmt_itr.hasNext()) {
      fout_writer.write(""+stmt_itr.next()+"\n");
      ++i_stmt;
    }
    System.err.println("statements: "+i_stmt);
  }

  /////////////////////////////////////////////////////////////////////////////
  private static int verbose=0;
  private static String ifile_ont=null;
  private static String url_ont=null;
  private static String [] ifiles_rdf=null;
  private static String ofile=null;
  private static Boolean describe_ontology=false;
  private static Boolean describe_rdf=false;
  private static Boolean list_subclasses=false;
  private static Boolean list_rootclasses=false;
  private static Boolean ont2graphml=false;
  private static Boolean ont2cyjs=false;
  private static Boolean ont2tsv=false;
  private static Boolean list_classes=false;
  private static String otype="OWL";
  private static Boolean validate_rdf=false;
  private static Boolean validate_ont=false;
  private static Boolean infer_all=false;
  private static Boolean query_rdf=false;
  private static Boolean query_endpoint=false;
  private static String dlang=null;
  private static String endpoint_url=null;
  private static String rq=null;
  private static String rqfile=null;

  private static void Help(String msg)
  {
    System.err.println(msg+"\n"
      +"jena_utils - Jena utility\n"
      +"\n"
      +"usage: jena_utils [options]\n"
      +"input:\n"
      +"  -rdffiles FILE1[,FILE2...]  ... data file[s] (TTL|N3|RDF/XML)\n"
      +"  -rq SPARQL .................... Sparql\n"
      +"  -rqfile RQFILE ................ Sparql file\n"
      +"  -endpoint_url URL ............. endpoint URL\n"
      +"  -ontfile FILE ................. ontology file (OWL|RDFS)\n"
      +"  -onturl URL ................... ontology URL (OWL|RDFS)\n"
      +"\n"
      +"operations:\n"
      +"  -describe_rdf ................. requires RDF\n"
      +"  -describe_ontology ............ requires ontology\n"
      +"  -list_classes ................. list ontology classes with labels\n"
      +"  -list_subclasses .............. list ontology subclass relationships\n"
      +"  -list_rootclasses ............. list ontology root classes\n"
      +"  -ont2graphml .................. convert ontology class hierarchy to GraphML format \n"
      +"  -ont2cyjs ..................... convert ontology class hierarchy to CYJS format \n"
      +"  -query_rdf .................... query RDF\n"
      +"  -query_endpoint ............... query endpoint URL\n"
      +"\n"
      +"reasoner operations (may be slow):\n"
      +"  -validate_rdf ................. validate data + ontology\n"
      +"  -validate_ont ................. validate ontology\n"
      +"  -infer_all .................... infer all statements, asserted and entailed\n"
      +"\n"
      +"options:\n"
      +"  -o OFILE ...................... normally CSV\n"
      +"  -ontology_type OTYPE .......... OWL|RDFS ["+otype+"]\n"
      +"  -dlang LANG ................... data language (RDF/XML|TTL|N3)\n"
      +"  -v[v[v]] ...................... verbose [very [very]]\n"
      +"  -h ............................ this help\n"
      +"\n"
      +"Notes:\n"
      +"  * 1st data file is default graph.\n"
      +"  * OWL must be RDF/XML format.\n"
      +"  * Validation can be slow and memory intensive.\n"
	);
    System.exit(1);
  }

  /////////////////////////////////////////////////////////////////////////////
  private static void ParseCommand(String args[])
  {
    if (args.length==0) Help("");
    for (int i=0;i<args.length;++i)
    {
      if (args[i].equals("-ontfile")) ifile_ont=args[++i];
      else if (args[i].equals("-onturl")) url_ont=args[++i];
      else if (args[i].equals("-rdffiles")) ifiles_rdf = Pattern.compile(",").split(args[++i]);
      else if (args[i].equals("-o")) ofile=args[++i];
      else if (args[i].equals("-describe_ontology")) describe_ontology=true;
      else if (args[i].equals("-describe_rdf")) describe_rdf=true;
      else if (args[i].equals("-list_classes")) list_classes=true;
      else if (args[i].equals("-list_subclasses")) list_subclasses=true;
      else if (args[i].equals("-list_rootclasses")) list_rootclasses=true;
      else if (args[i].equals("-ont2graphml")) ont2graphml=true;
      else if (args[i].equals("-ont2cyjs")) ont2cyjs=true;
      else if (args[i].equals("-ont2tsv")) ont2tsv=true;
      else if (args[i].equals("-ontology_type")) otype=args[++i];
      else if (args[i].equals("-validate_rdf")) validate_rdf=true;
      else if (args[i].equals("-validate_ont")) validate_ont=true;
      else if (args[i].equals("-infer_all")) infer_all=true;
      else if (args[i].equals("-query_rdf")) query_rdf=true;
      else if (args[i].equals("-query_endpoint")) query_endpoint=true;
      else if (args[i].equals("-dlang")) dlang=args[++i];
      else if (args[i].equals("-endpoint_url")) endpoint_url=args[++i];
      else if (args[i].equals("-rq")) rq=args[++i];
      else if (args[i].equals("-rqfile")) rqfile=args[++i];
      else if (args[i].equals("-v")) verbose=1;
      else if (args[i].equals("-vv")) verbose=2;
      else if (args[i].equals("-vvv"))verbose=3;
      else if (args[i].equals("-h")) Help("");
      else Help("Unknown option: "+args[i]);
    }
  }
  /////////////////////////////////////////////////////////////////////////////
  public static void main(String [] args)
        throws Exception
  {
    ParseCommand(args);

    //String jenapropfile = null;
    if (Files.isReadable(FileSystems.getDefault().getPath("src/main/resources", "jena-log4j.properties")))
      LogCtl.setLog4j("src/main/resources/jena-log4j.properties");
    else if (Files.isReadable(FileSystems.getDefault().getPath("/home/app/apache-jena", "jena-log4j.properties")))
      LogCtl.setLog4j("/home/app/apache-jena/jena-log4j.properties");
    else
      LogCtl.setLog4j();

    //if (verbose>1) System.err.println("jenapropfile: "+jenapropfile);
    //LogCtl.setLog4j(jenapropfile);

    PrintWriter fout_writer = (ofile!=null) ?
      new PrintWriter(new BufferedWriter(new FileWriter(new File(ofile),false)))
      : new PrintWriter((OutputStream)System.out);

    if (verbose>0)
    {
      System.err.println("Jena version: "+jena.version.VERSION);
      //System.err.println("Jena-ARQ version: "+ARQ.VERSION);
    }

    if (rqfile!=null) //read file into rq
    {
      if (verbose>0)
        System.err.println("rqfile: "+rqfile);
      BufferedReader buff = new BufferedReader(new FileReader(rqfile));
      if (buff==null) Help("Cannot open sparql file: "+rqfile);
      String line="";
      rq="";
      while ((line=buff.readLine())!=null) rq+=(line+"\n");
      buff.close();
    }

    java.util.Date t_0 = new java.util.Date();

    // Input ontology
    OntModel omod = null;
    if (ifile_ont!=null)
      omod = LoadOntologyFile(ifile_ont,otype,verbose);
    else if (url_ont!=null)
      omod = LoadOntologyUrl(url_ont,otype,verbose);

    // Input dataset (1+ files, models, graphs)
    Model [] rmods = null;
    Dataset dset = null; //for named graphs
    if (ifiles_rdf!=null) {
      rmods = new Model[ifiles_rdf.length];
      dset = DatasetFactory.createMem();
      LoadRDF(ifiles_rdf, rmods, dset,verbose);
    }

    if (verbose>0 && dset!=null) {
      DescribeDataset(dset,verbose);
    }

    if (describe_ontology) {
      if (omod==null) Help("-ontfile required");
      DescribeOntology(omod,verbose);
    }
    else if (describe_rdf) {
      if (rmods==null) Help("-rdffiles required");
      for (Model rmod: rmods)
      {
        DescribeRDF(rmod,verbose);
      }
    }
    else if (validate_rdf) {
      if (rmods==null) Help("-rdffiles required");
      Reasoner reasoner = null;
      if (omod!=null) {
        reasoner = ReasonerRegistry.getOWLReasoner();
        reasoner = reasoner.bindSchema(omod);
      }
      for (Model rmod: rmods) {
        ValidateModel(rmod,reasoner);
      }
    }
    else if (validate_ont) {
      if (omod==null) Help("-ontfile required");
      ValidateModel(omod,null);
    }
    else if (infer_all) {
      if (rmods==null) Help("-rdffiles required");
      if (omod==null) Help("-ontfile required");
      Reasoner reasoner = ReasonerRegistry.getTransitiveReasoner();
      reasoner = reasoner.bindSchema(omod);
      for (Model rmod: rmods) {
        InferAllStatements(rmod,reasoner,fout_writer,verbose);
      }
    }
    else if (list_classes) {
      if (omod==null) Help("-ontfile required");
      OntModelClassList(omod,fout_writer,verbose);
    }
    else if (list_subclasses) {
      if (omod==null) Help("-ontfile required");
      OntModelSubclassList(omod,fout_writer,verbose);
    }
    else if (list_rootclasses) {
      if (omod==null) Help("-ontfile required");
      OntModelRootclassList(omod,fout_writer,verbose);
    }
    else if (ont2graphml) {
      if (omod==null) Help("-ontfile required");
      OntModel2GraphML(omod,fout_writer,verbose);
    }
    else if (ont2cyjs) {
      if (omod==null) Help("-ontfile required");
      OntModel2CYJS(omod,fout_writer,verbose);
    }
    else if (ont2tsv) {
      if (omod==null) Help("-ontfile required");
      OntModel2TSV(omod,fout_writer,verbose);
    }
    else if (query_rdf) {
      if (dset==null) Help("-rdffiles required");
      if (rq==null) Help("-rq or -rqfile required");
      QueryRDF(dset,rq,fout_writer,verbose);
    }
    else if (query_endpoint) { // Execute SELECT query using specified endpoint URL.
      if (endpoint_url==null) Help("-endpoint_url required");
      if (rq==null) Help("-rq or -rqfile required");
      QueryEndpoint(endpoint_url,rq,fout_writer,verbose);
    }
    else {
      Help("ERROR: no operation specified.");
    }
    fout_writer.close();
  }
}
