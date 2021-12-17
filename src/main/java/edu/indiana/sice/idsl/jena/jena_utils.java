package edu.indiana.sice.idsl.jena;

import java.io.*;
import java.nio.file.*; //Files, Path
import java.util.*; //Collections, Properties
import java.util.regex.*;
import java.net.*; //URL

import org.apache.commons.cli.*; // CommandLine, CommandLineParser, HelpFormatter, OptionBuilder, Options, ParseException, PosixParser
import org.apache.commons.cli.Option.*; // Builder

import org.apache.jena.Jena; // VERSION
import org.apache.jena.rdf.model.*; // Model
import org.apache.jena.ontology.*; // OntModel
import org.apache.jena.reasoner.*; // Reasoner, ReasonerRegistry, InfModel
import org.apache.jena.query.*; //ARQ, Dataset, DatasetFactory
import org.apache.jena.util.*; // FileManager,
import org.apache.jena.util.iterator.*; // ExtendedIterator
import org.apache.jena.sparql.*; //Sparql
import org.apache.jena.sparql.core.*; //Prologue
import org.apache.jena.sparql.engine.http.*; //QueryEngineHTTP

import com.fasterxml.jackson.core.*; //JsonFactory, JsonGenerator
import com.fasterxml.jackson.databind.*; //ObjectMapper, JsonNode

import org.apache.jena.atlas.logging.LogCtl;

/**	Jena utility functions and app.
	@author Jeremy Yang
*/
public class jena_utils
{
  /////////////////////////////////////////////////////////////////////////////
  /**	First file should be default graph.
  */
  public static void LoadRDF(String [] ifiles_rdf, Model [] rmods, Dataset dset)
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
    ExtendedIterator<OntClass> cls_itr = omod.listClasses();
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
    ExtendedIterator<OntClass> cls_itr = omod.listClasses();
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
    ExtendedIterator<OntClass> cls_itr = omod.listClasses();
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
    ExtendedIterator<OntClass> cls_itr = omod.listClasses();
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
  private static String CleanLabel(String label)
  {
    if (label==null) return(null);
    String label_clean = label.replaceFirst("[\\s]+$","").replaceAll("&","&amp;").replaceAll("<","&lt;").replaceAll(">","&gt;").replaceAll("\"","&quot;").replaceAll("[\\t\\n\\r]"," ");
    return (label_clean);
  }
  private static String CleanComment(String comment)
  {
    if (comment==null) return(null);
    String comment_clean = comment.replaceAll("&","&amp;").replaceAll("<","&lt;").replaceAll(">","&gt;").replaceAll("\"","&quot;").replaceAll("[\\t\\n\\r]"," ");
    return (comment_clean);
  }
  private static String Uri2Id(String uri)
  {
    if (uri==null) return(null);
    return (uri.replaceFirst("^.*/", ""));
  }

  /////////////////////////////////////////////////////////////////////////////
  /**	For all classes, identify and output level [0-maxlevel] superclasses (0=root).
  */
  public static void ListToplevelSuperclassMembership(OntModel omod, PrintWriter fout_writer, int maxlevel, int verbose) throws Exception
  {
    fout_writer.write("id\tlabel\turi");
    for (int j=0; j<=maxlevel; ++j) fout_writer.write("\tSuperClassUriLev_"+j+"\tSuperClassLabelLev_"+j);
    fout_writer.write("\n");
    ExtendedIterator<OntClass> cls_itr = omod.listClasses(); //All classes.
    int i_cls=0;
    while (cls_itr.hasNext())
    {
      OntClass cls = cls_itr.next();
      String uri = cls.getURI();
      if (uri==null) continue; //error
      String id = Uri2Id(uri);
      String label = CleanLabel(cls.getLabel(null));
      fout_writer.write(String.format("%s\t%s\t%s", id, ((label!=null)?label:id), uri)); 
      ArrayList<OntClass> sups = GetSuperclassListMinimal(cls);
      if (sups!=null) Collections.reverse(sups); //Reverse so root is 1st.
      for (int j=0; j<=maxlevel; ++j) {
        if (sups!=null && j<sups.size()) {
          uri = sups.get(j).getURI();
          id = Uri2Id(uri);
          label = CleanLabel(sups.get(j).getLabel(null));
          fout_writer.write("\t"+uri+"\t"+((label!=null)?label:id));
        }
        else { fout_writer.write("\t"); }
      }
      fout_writer.write("\n");
      ++i_cls;
    }
    System.err.println("nodes (classes): "+i_cls);
  }

  /////////////////////////////////////////////////////////////////////////////
  /**	Return minimum length superclass list, ordered from root.
  */
  public static ArrayList<OntClass> GetSuperclassListMinimal(OntClass cls) throws Exception
  {
    ArrayList<OntClass> sups_min = null;
    ArrayList<ArrayList<OntClass> > supss = GetSuperclassLists(cls);
    for (int i=0; i<supss.size(); ++i) {
      ArrayList<OntClass> sups_this = supss.get(i);
      if (sups_min==null || sups_this.size()<sups_min.size()) sups_min = sups_this;
    }
    return (sups_min);
  }

  /////////////////////////////////////////////////////////////////////////////
  /**	Return list of superclass lists, ordered from adjacent parent to root.
	Depth first search via recursion.
	Multiple parents possible, in multi-hierarchy.
	Minimum list length is level in class hierarchy.
  */
  public static ArrayList<ArrayList<OntClass> > GetSuperclassLists(OntClass cls) throws Exception
  {
    ArrayList<ArrayList<OntClass> > supss = new ArrayList<ArrayList<OntClass> >(); //List of superclass lists (LoL), all parents
    //if (!cls.hasSuperClass()) return(supss); //No parents, no lists, return 0-length LoL.
    Boolean ok=false;
    try { ok = cls.hasSuperClass(); } //Why exception? Why/when not OntClass??
    catch (Exception e) {
      System.err.println("DEBUG: cls.getClass().getName(): "+cls.getClass().getName());
      System.err.println(e.getMessage());
    }
    if (!ok) return(supss); //No parents, no lists, return 0-length LoL.
    ExtendedIterator<OntClass> sup_itr = cls.listSuperClasses(true); // direct - only classes directly adjacent in hierarchy.
    while (sup_itr.hasNext()) //For each parent, recurse.
    {
      OntClass sup = sup_itr.next();
      ArrayList<ArrayList<OntClass> > supss_this = GetSuperclassLists(sup);
      if (supss_this.size()==0) {
        ArrayList<OntClass> sups = new ArrayList<OntClass>(); //Superclass list, this parent, length 1.
        sups.add(sup);
        supss.add(sups);
      }
      for (int i=0; i<supss_this.size(); ++i) {
        ArrayList<OntClass> sups = new ArrayList<OntClass>(); //Superclass list, this parent
        sups.add(sup);
        sups.addAll(supss_this.get(i));
        supss.add(sups);
      }
    }
    return (supss);
  }

  /////////////////////////////////////////////////////////////////////////////
  /**	Converts ontology class hierarchy to TSV.
  */
  public static void OntModel2TSV(OntModel omod, PrintWriter fout_writer, int verbose) throws Exception {
    fout_writer.write("node_or_edge\tid\tlabel\tcomment\tsource\ttarget\turi\n");
    ExtendedIterator<OntClass> cls_itr = omod.listClasses(); //All classes.
    int i_cls=0;
    int i_subcls=0;
    while (cls_itr.hasNext())
    {
      OntClass cls = cls_itr.next();
      String uri=cls.getURI();
      if (uri==null) continue; //error
      String id = Uri2Id(uri);
      String label = CleanLabel(cls.getLabel(null));
      String comment = CleanComment(cls.getComment(null));
      fout_writer.write(String.format("node\t%s\t%s\t%s\t\t\t%s\n", id, label, comment, uri)); 
      ++i_cls;
    }
    cls_itr = omod.listClasses(); //rewind for subclasses/edges
    while (cls_itr.hasNext())
    {
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
  /**	Converts ontology to edge list suitable for Pandas/NetworkX.
	https://networkx.github.io/documentation/stable/reference/generated/networkx.convert_matrix.from_pandas_edgelist.html
  */
  public static void OntModel2Edgelist(OntModel omod, PrintWriter fout_writer, int verbose) throws Exception
  {
    fout_writer.write("source\ttarget\tedge_attr\n");
    HashSet<String> ids = new HashSet<String>();
    ExtendedIterator<OntClass> cls_itr=omod.listClasses(); //All classes.
    int i_edge=0;
    while (cls_itr.hasNext())
    {
      OntClass cls=cls_itr.next();
      String uri=cls.getURI();
      if (uri==null) continue; //error
      String id=uri.replaceFirst("^.*/","");
      ids.add(id);
      ExtendedIterator<OntClass> subcls_itr = cls.listSubClasses();
      while (subcls_itr.hasNext())
      {
        OntClass subcls = subcls_itr.next();
        String uri_sub=subcls.getURI(); //target
        if (uri_sub==null) continue; //error
        String id_sub=uri_sub.replaceFirst("^.*/","");
        String edge_attr="has_subclass";
        fout_writer.write(String.format("%s\t%s\t%s\n", id, id_sub, edge_attr)); 
        ++i_edge;
      }
    }
    System.err.println("nodes (classes): "+ids.size()+", edges (has_subclass): "+i_edge);
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	Converts ontology classes to node list suitable for Pandas/NetworkX.
  */
  public static void OntModel2Nodelist(OntModel omod, PrintWriter fout_writer, int verbose) throws Exception
  {
    fout_writer.write("id\turi\tlabel\tcomment\n");
    HashSet<String> ids = new HashSet<String>();
    ExtendedIterator<OntClass> cls_itr=omod.listClasses(); //All classes.
    int i_cls=0;
    while (cls_itr.hasNext())
    {
      OntClass cls=cls_itr.next();
      String uri=cls.getURI();
      if (uri==null) continue; //error
      String id=uri.replaceFirst("^.*/","");
      ids.add(id);
      String label = CleanLabel(cls.getLabel(null));
      String comment = CleanComment(cls.getComment(null));
      fout_writer.write(String.format("%s\t%s\t%s\t%s\n", id, uri, label, comment)); 
      ++i_cls;
    }
    System.err.println("nodes (classes): "+ids.size());
  }

  /////////////////////////////////////////////////////////////////////////////
  /**	Converts ontology class hierarchy to a Cytoscape JS format directed
	graph for processing and viewing.  Use Jackson-databind library.
  */
  public static void OntModel2CYJS(OntModel omod, PrintWriter fout_writer, int verbose) throws Exception
  {
    ExtendedIterator<OntClass> cls_itr = omod.listClasses();
    int i_cls=0;
    int i_subcls=0;

    HashMap<String,Object> root = new HashMap<String,Object>();
    root.put("format_version", "1.0");
    root.put("generated_by", "edu.indiana.sice.idsl.jena.jena_utils");
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
      String label = CleanLabel(cls.getLabel(null));
      String comment = CleanComment(cls.getComment(null));
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
    cls_itr = omod.listClasses(); //rewind for subclasses/edges
    while (cls_itr.hasNext())
    {
      OntClass cls = cls_itr.next();
      String uri=cls.getURI();
      if (uri==null) continue; //error
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
    ExtendedIterator<OntClass> cls_itr = omod.listClasses();
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
      String label = CleanLabel(cls.getLabel(null));
      String comment = CleanComment(cls.getComment(null));
      fout_writer.write(
"    <node id=\""+id+"\">\n"
+"      <data key=\"uri\">"+uri+"</data>\n"
+"      <data key=\"name\">"+label+"</data>\n"
+"      <data key=\"comment\">"+comment+"</data>\n"
+"    </node>\n"
	);
      ++i_cls;
    }

    cls_itr = omod.listClasses(); //rewind for subclasses/edges
    while (cls_itr.hasNext())
    {
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
  private static String APPNAME="JENA_APP";
  //Required (one of):
  private static String ifile_ont=null;
  private static String url_ont=null;
  //Options:
  private static String endpoint_url=null;
  private static String sparql=null;
  private static String sparqlfile=null;
  private static String [] ifiles_rdf=null;
  private static String dlang=null;
  private static String ofile=null;
  private static String otype="OWL";
  private static int maxlevel=5;
  private static int verbose=0;
  //Operations:
  private static Boolean describe_ontology=false;
  private static Boolean describe_rdf=false;
  private static Boolean validate_ontology=false;
  private static Boolean validate_rdf=false;
  private static Boolean list_classes=false;
  private static Boolean list_subclasses=false;
  private static Boolean list_rootclasses=false;
  private static Boolean list_toplevelsuperclassmembership=false;
  private static Boolean query_rdf=false;
  private static Boolean query_endpoint=false;
  private static Boolean ont2graphml=false;
  private static Boolean ont2cyjs=false;
  private static Boolean ont2edgelist=false;
  private static Boolean ont2nodelist=false;
  private static Boolean ont2tsv=false;
  private static Boolean infer_all=false;

  /////////////////////////////////////////////////////////////////////////////
  public static void main(String [] args) throws Exception
  {
    String HELPHEADER =  "JENA_APP: Jena utilities";
    String HELPFOOTER = ("Notes:\n"
      +"  * 1st data file is default graph.\n"
      +"  * OWL must be RDF/XML format.\n"
      +"  * Reasoner operations may be slow.\n"
      +"  * Validation can be slow and memory intensive.");
    Options opts = new Options();
    opts.addOption(Option.builder("ifile_ont").hasArg().argName("IFILE_ONT").desc("Input ontology file (OWL|RDFS)").build());
    opts.addOption(Option.builder("url_ont").hasArg().argName("URL_ONT").desc("Input ontology URL (OWL|RDFS)").build());
    opts.addOption(Option.builder("sparql").hasArg().argName("SPARQL").desc("Sparql").build());
    opts.addOption(Option.builder("sparqlfile").hasArg().argName("SPARQLFILE").desc("Sparql file").build());
    opts.addOption(Option.builder("endpoint_url").hasArg().argName("URL").desc("endpoint URL").build());
    opts.addOption(Option.builder("rdffiles").hasArg().argName("RDFFILES").desc("data file[s], comma-separated list (TTL|N3|RDF/XML)").build());
    opts.addOption(Option.builder("o").longOpt("ofile").hasArg().argName("OFILE").desc("Output file").build());

    opts.addOption(Option.builder("describe_rdf").desc("requires RDF").build());
    opts.addOption(Option.builder("describe_ontology").desc("requires ontology").build());
    opts.addOption(Option.builder("list_classes").desc("list ontology classes with labels").build());
    opts.addOption(Option.builder("list_subclasses").desc("list ontology subclass relationships").build());
    opts.addOption(Option.builder("list_rootclasses").desc("list ontology root classes").build());
    opts.addOption(Option.builder("list_toplevelsuperclassmembership").desc("list for all classes superclass membership, top level [0-"+maxlevel+"]").build());
    opts.addOption(Option.builder("ont2graphml").desc("convert ontology class hierarchy to GraphML format ").build());
    opts.addOption(Option.builder("ont2cyjs").desc("convert ontology class hierarchy to CYJS format ").build());
    opts.addOption(Option.builder("ont2edgelist").desc("convert ontology class hierarchy to Pandas/NetworkX edgelist").build());
    opts.addOption(Option.builder("ont2nodelist").desc("convert ontology class hierarchy to Pandas/NetworkX nodelist").build());
    opts.addOption(Option.builder("ont2tsv").desc("convert ontology class hierarchy to TSV").build());
    opts.addOption(Option.builder("query_rdf").desc("query RDF").build());
    opts.addOption(Option.builder("query_endpoint").desc("query endpoint URL").build());
    opts.addOption(Option.builder("validate_rdf").desc("validate data + ontology").build());
    opts.addOption(Option.builder("validate_ontology").desc("validate ontology").build());
    opts.addOption(Option.builder("infer_all").desc("infer all statements, asserted and entailed").build());

    opts.addOption(Option.builder("ontology_type").hasArg().argName("OTYPE").desc("OWL|RDFS ["+otype+"]").build());
    opts.addOption(Option.builder("dlang").hasArg().argName("LANG").desc("data language (RDF/XML|TTL|N3)").build());

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
    if (clic.hasOption("ifile_ont")) ifile_ont = clic.getOptionValue("ifile_ont");
    else if (clic.hasOption("url_ont")) url_ont = clic.getOptionValue("url_ont");
    else {
      helper.printHelp(APPNAME, HELPHEADER, opts, ("-ifile_ont or -url_ont requied."), true);
      System.exit(0);
    }
    if (clic.hasOption("o")) ofile = clic.getOptionValue("o");
    if (clic.hasOption("dlang")) dlang = clic.getOptionValue("dlang");
    if (clic.hasOption("otype")) otype = clic.getOptionValue("otype");
    if (clic.hasOption("endpoint_url")) endpoint_url = clic.getOptionValue("endpoint_url");
    if (clic.hasOption("sparql")) sparql = clic.getOptionValue("sparql");
    if (clic.hasOption("sparqlfile")) sparqlfile = clic.getOptionValue("sparqlfile");
    if (clic.hasOption("rdffiles")) ifiles_rdf = Pattern.compile("[\\s,]+").split(clic.getOptionValue("rdffiles"));
    if (clic.hasOption("describe_ontology")) describe_ontology = true;
    if (clic.hasOption("describe_rdf")) describe_rdf=true;
    if (clic.hasOption("list_subclasses")) list_subclasses=true;
    if (clic.hasOption("list_rootclasses")) list_rootclasses=true;
    if (clic.hasOption("list_toplevelsuperclassmembership")) list_toplevelsuperclassmembership=true;
    if (clic.hasOption("ont2graphml")) ont2graphml=true;
    if (clic.hasOption("ont2cyjs")) ont2cyjs=true;
    if (clic.hasOption("ont2edgelist")) ont2edgelist=true;
    if (clic.hasOption("ont2nodelist")) ont2nodelist=true;
    if (clic.hasOption("ont2tsv")) ont2tsv=true;
    if (clic.hasOption("list_classes")) list_classes=true;
    if (clic.hasOption("validate_rdf")) validate_rdf=true;
    if (clic.hasOption("validate_ontology")) validate_ontology=true;
    if (clic.hasOption("infer_all")) infer_all=true;
    if (clic.hasOption("query_rdf")) query_rdf=true;
    if (clic.hasOption("query_endpoint")) query_endpoint=true;
    if (clic.hasOption("vvv")) verbose = 3;
    else if (clic.hasOption("vv")) verbose = 2;
    else if (clic.hasOption("v")) verbose = 1;
    if (clic.hasOption("h")) {
      helper.printHelp(APPNAME, HELPHEADER, opts, HELPFOOTER, true);
      System.exit(0);
    }

    //String jenapropfile = null;
    //if (Files.isReadable(FileSystems.getDefault().getPath(System.getenv("HOME")+"/../app/apache-jena", "jena-log4j.properties")))
    //  LogCtl.setLog4j(System.getenv("HOME")+"/../app/apache-jena/jena-log4j.properties");
    //else if (Files.isReadable(FileSystems.getDefault().getPath("src/main/resources", "jena-log4j.properties")))
    //  LogCtl.setLog4j("src/main/resources/jena-log4j.properties");
    //else
    //  LogCtl.setLog4j();

    PrintWriter fout_writer = (ofile!=null)?(new PrintWriter(new BufferedWriter(new FileWriter(new File(ofile),false)))):(new PrintWriter((OutputStream)System.out));

    if (verbose>0)
      System.err.println("Jena version: "+Jena.VERSION);

    if (sparqlfile!=null) //read file into rq
    {
      if (verbose>0)
        System.err.println("sparqlfile: "+sparqlfile);
      BufferedReader buff = new BufferedReader(new FileReader(sparqlfile));
      if (buff==null)
      helper.printHelp(APPNAME, HELPHEADER, opts, ("Cannot open sparql file: "+sparqlfile), true);
      String line="";
      sparql="";
      while ((line=buff.readLine())!=null) sparql+=(line+"\n");
      buff.close();
    }

    java.util.Date t_0 = new java.util.Date();

    // Input ontology
    OntModel omod = null;
    if (ifile_ont!=null)
      omod = LoadOntologyFile(ifile_ont, otype, verbose);
    else if (url_ont!=null)
      omod = LoadOntologyUrl(url_ont, otype, verbose);

    // Input dataset (1+ files, models, graphs)
    Model [] rmods = null;
    Dataset dset = null; //for named graphs
    if (ifiles_rdf!=null) {
      rmods = new Model[ifiles_rdf.length];
      dset = DatasetFactory.create();
      LoadRDF(ifiles_rdf, rmods, dset);
    }

    if (verbose>0 && dset!=null) {
      DescribeDataset(dset, verbose);
    }

    if (describe_ontology) {
      if (omod==null)
        helper.printHelp(APPNAME, HELPHEADER, opts, ("-ifile_ont or -url_ont required"), true);
      DescribeOntology(omod, verbose);
    }
    else if (describe_rdf) {
      if (rmods==null)
        helper.printHelp(APPNAME, HELPHEADER, opts, ("-rdffiles required"), true);
      for (Model rmod: rmods)
      {
        DescribeRDF(rmod, verbose);
      }
    }
    else if (validate_rdf) {
      if (rmods==null)
        helper.printHelp(APPNAME, HELPHEADER, opts, ("-rdffiles required"), true);
      Reasoner reasoner = null;
      if (omod!=null) {
        reasoner = ReasonerRegistry.getOWLReasoner();
        reasoner = reasoner.bindSchema(omod);
      }
      for (Model rmod: rmods) {
        ValidateModel(rmod, reasoner);
      }
    }
    else if (validate_ontology) {
      if (omod==null)
        helper.printHelp(APPNAME, HELPHEADER, opts, ("-ifile_ont or -url_ont required"), true);
      ValidateModel(omod, null);
    }
    else if (infer_all) {
      if (rmods==null)
        helper.printHelp(APPNAME, HELPHEADER, opts, ("-rdffiles required"), true);
      if (omod==null)
        helper.printHelp(APPNAME, HELPHEADER, opts, ("-ifile_ont required"), true);
      Reasoner reasoner = ReasonerRegistry.getTransitiveReasoner();
      reasoner = reasoner.bindSchema(omod);
      for (Model rmod: rmods) {
        InferAllStatements(rmod, reasoner, fout_writer, verbose);
      }
    }
    else if (list_classes) {
      if (omod==null) helper.printHelp(APPNAME, HELPHEADER, opts, ("-ifile_ont or -url_ont required"), true);
      OntModelClassList(omod, fout_writer, verbose);
    }
    else if (list_subclasses) {
      if (omod==null) helper.printHelp(APPNAME, HELPHEADER, opts, ("-ifile_ont or -url_ont required"), true);
      OntModelSubclassList(omod, fout_writer, verbose);
    }
    else if (list_rootclasses) {
      if (omod==null) helper.printHelp(APPNAME, HELPHEADER, opts, ("-ifile_ont or -url_ont required"), true);
      OntModelRootclassList(omod, fout_writer, verbose);
    }
    else if (list_toplevelsuperclassmembership) {
      if (omod==null) helper.printHelp(APPNAME, HELPHEADER, opts, ("-ifile_ont or -url_ont required"), true);
      ListToplevelSuperclassMembership(omod, fout_writer, maxlevel, verbose);
    }
    else if (ont2graphml) {
      if (omod==null) helper.printHelp(APPNAME, HELPHEADER, opts, ("-ifile_ont or -url_ont required"), true);
      OntModel2GraphML(omod, fout_writer, verbose);
    }
    else if (ont2cyjs) {
      if (omod==null) helper.printHelp(APPNAME, HELPHEADER, opts, ("-ifile_ont or -url_ont required"), true);
      OntModel2CYJS(omod, fout_writer, verbose);
    }
    else if (ont2tsv) {
      if (omod==null) helper.printHelp(APPNAME, HELPHEADER, opts, ("-ifile_ont or -url_ont required"), true);
      OntModel2TSV(omod, fout_writer, verbose);
    }
    else if (ont2edgelist) {
      if (omod==null) helper.printHelp(APPNAME, HELPHEADER, opts, ("-ifile_ont or -url_ont required"), true);
      OntModel2Edgelist(omod, fout_writer, verbose);
    }
    else if (ont2nodelist) {
      if (omod==null) helper.printHelp(APPNAME, HELPHEADER, opts, ("-ifile_ont or -url_ont required"), true);
      OntModel2Nodelist(omod, fout_writer, verbose);
    }
    else if (query_rdf) {
      if (dset==null) helper.printHelp(APPNAME, HELPHEADER, opts, ("-rdffiles required"), true);
      if (sparql==null) helper.printHelp(APPNAME, HELPHEADER, opts, ("-sparql or -sparqlfile required"), true);
      QueryRDF(dset, sparql, fout_writer, verbose);
    }
    else if (query_endpoint) { // Execute SELECT query using specified endpoint URL.
      if (endpoint_url==null) helper.printHelp(APPNAME, HELPHEADER, opts, ("-endpoint_url required"), true);
      if (sparql==null) helper.printHelp(APPNAME, HELPHEADER, opts, ("-sparql or -sparqlfile required"), true);
      QueryEndpoint(endpoint_url, sparql, fout_writer, verbose);
    }
    else {
      helper.printHelp(APPNAME, HELPHEADER, opts, ("ERROR: no operation specified."), true);
    }
    fout_writer.close();
  }
}
