package org.apache.lucene.demo;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.es.SpanishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.Date;

/** Index all text files under a directory.
 * <p>
 * This is a command-line application demonstrating simple Lucene indexing.
 * Run it with no command-line arguments for usage information.
 */
public class IndexFiles {

  private IndexFiles() {}

  /** Index all text files under a directory. */
  public static void main(String[] args) {
    String usage = "java org.apache.lucene.demo.IndexFiles"
            + " [-index INDEX_PATH] [-docs DOCS_PATH] [-update]\n\n"
            + "This indexes the documents in DOCS_PATH, creating a Lucene index"
            + "in INDEX_PATH that can be searched with SearchFiles";
    String indexPath = "index";
    String docsPath = null;
    boolean create = true;
    for(int i=0;i<args.length;i++) {
      if ("-index".equals(args[i])) {
        indexPath = args[i+1];
        i++;
      } else if ("-docs".equals(args[i])) {
        docsPath = args[i+1];
        i++;
      } else if ("-update".equals(args[i])) {
        create = false;
      }
    }

    if (docsPath == null) {
      System.err.println("Usage: " + usage);
      System.exit(1);
    }

    final File docDir = new File(docsPath);
    if (!docDir.exists() || !docDir.canRead()) {
      System.out.println("Document directory '" +docDir.getAbsolutePath()+ "' does not exist or is not readable, please check the path");
      System.exit(1);
    }

    Date start = new Date();
    try {
      System.out.println("Indexing to directory '" + indexPath + "'...");

      Directory dir = FSDirectory.open(Paths.get(indexPath));
      Analyzer analyzer = new SpanishAnalyzer2();
      //Analyzer analyzer = new WhitespaceAnalyzer();
      IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

      if (create) {
        // Create a new index in the directory, removing any
        // previously indexed documents:
        iwc.setOpenMode(OpenMode.CREATE);
      } else {
        // Add new documents to an existing index:
        iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
      }

      // Optional: for better indexing performance, if you
      // are indexing many documents, increase the RAM
      // buffer.  But if you do this, increase the max heap
      // size to the JVM (eg add -Xmx512m or -Xmx1g):
      //
      // iwc.setRAMBufferSizeMB(256.0);

      IndexWriter writer = new IndexWriter(dir, iwc);
      indexDocs(writer, docDir);

      // NOTE: if you want to maximize search performance,
      // you can optionally call forceMerge here.  This can be
      // a terribly costly operation, so generally it's only
      // worth it when your index is relatively static (ie
      // you're done adding documents to it):
      //
      // writer.forceMerge(1);

      writer.close();

      Date end = new Date();
      System.out.println(end.getTime() - start.getTime() + " total milliseconds");

    } catch (IOException e) {
      System.out.println(" caught a " + e.getClass() +
              "\n with message: " + e.getMessage());
    }
  }

  static void ajustadoText(File file, Document doc, String etiqueta){
    DocumentBuilderFactory dcf = DocumentBuilderFactory.newDefaultInstance();
    DocumentBuilder dcb;
    try {dcb = dcf.newDocumentBuilder();}
    catch (ParserConfigurationException e) {throw new RuntimeException(e);}

    org.w3c.dom.Document dom;
    try { dom = dcb.parse(file);}
    catch (SAXException | IOException e) {throw new RuntimeException(e);}

    NodeList listNodes = dom.getElementsByTagName("dc:" + etiqueta);

    //System.out.println();
    //para cada lista recorro, para cada nodo coger contenido guardar el el doc
    for(int i = 0; i < listNodes.getLength(); i++){

      //System.out.println(listNodes.item(i).getNodeName());
      Node nodo = listNodes.item(i);
      //System.out.println(nodo.get());
      doc.add(new TextField(etiqueta, nodo.getTextContent(),Field.Store.YES));
    }
  }

  static void ajustadoString(File file, Document doc, String etiqueta){
    DocumentBuilderFactory dcf = DocumentBuilderFactory.newDefaultInstance();
    DocumentBuilder dcb;
    try {dcb = dcf.newDocumentBuilder();}
    catch (ParserConfigurationException e) {throw new RuntimeException(e);}

    org.w3c.dom.Document dom;
    try { dom = dcb.parse(file);}
    catch (SAXException | IOException e) {throw new RuntimeException(e);}

    NodeList listNodes = dom.getElementsByTagName("dc:" + etiqueta);

    //System.out.println();
    //para cada lista recorro, para cada nodo coger contenido guardar el el doc
    for(int i = 0; i < listNodes.getLength(); i++){

      //System.out.println(listNodes.item(i).getNodeName());
      Node nodo = listNodes.item(i);
      //System.out.println(nodo.getTextContent());

      Field aux = new StringField(etiqueta, nodo.getTextContent(),Field.Store.YES);
      doc.add(aux);
    }
  }

  static void type(File file, Document doc, String etiqueta){
    DocumentBuilderFactory dcf = DocumentBuilderFactory.newDefaultInstance();
    DocumentBuilder dcb;
    try {dcb = dcf.newDocumentBuilder();}
    catch (ParserConfigurationException e) {throw new RuntimeException(e);}

    org.w3c.dom.Document dom;
    try { dom = dcb.parse(file);}
    catch (SAXException | IOException e) {throw new RuntimeException(e);}

    NodeList listNodes = dom.getElementsByTagName("dc:" + etiqueta);

    //System.out.println();
    //para cada lista recorro, para cada nodo coger contenido guardar el el doc
    for(int i = 0; i < listNodes.getLength(); i++){

      //System.out.println(listNodes.item(i).getNodeName());
      Node nodo = listNodes.item(i);
      //System.out.println(nodo.getTextContent());

      String result = nodo.getTextContent();
      if(result.contains("TFG")){
        Field aux = new StringField(etiqueta, "TFG",Field.Store.NO);
        doc.add(aux);
      } else if(result.contains("TFM")){
        Field aux = new StringField(etiqueta, "TFM",Field.Store.NO);
        doc.add(aux);
      } else if(result.contains("TESIS")){
        Field aux = new StringField(etiqueta, "tesis",Field.Store.NO);
        doc.add(aux);
      } else {
        Field aux = new StringField(etiqueta, result,Field.Store.NO);
        doc.add(aux);
      }

    }
  }


  static void coordenadas(File file, Document doc, String etiqueta){
    DocumentBuilderFactory dcf = DocumentBuilderFactory.newDefaultInstance();
    DocumentBuilder dcb;
    try {dcb = dcf.newDocumentBuilder();}
    catch (ParserConfigurationException e) {throw new RuntimeException(e);}

    org.w3c.dom.Document dom;
    try { dom = dcb.parse(file);}
    catch (SAXException | IOException e) {throw new RuntimeException(e);}

    NodeList listNodes = dom.getElementsByTagName("ows:" + etiqueta);

    //System.out.println();
    //para cada lista recorro, para cada nodo coger contenido guardar el el doc
    for(int i = 0; i < listNodes.getLength(); i++){

      //System.out.println(listNodes.item(i).getNodeName());
      Node nodo = listNodes.item(i);
      //System.out.println(nodo.getTextContent());
      // Oeste Sur
      // Este Norte
      if(etiqueta == "LowerCorner"){
        String[] Lower = nodo.getTextContent().split(" ");

        Double west = Double.parseDouble(Lower[0]);
        DoublePoint field = new DoublePoint ("west", west );
        doc.add(field);
        Double south = Double.parseDouble(Lower[1]);
        field = new DoublePoint ("south", south );
        doc.add(field);

      } else if (etiqueta == "UpperCorner") {
        String[] Upper = nodo.getTextContent().split(" ");
        Double east = Double.parseDouble(Upper[0]);
        Double north = Double.parseDouble(Upper[1]);

        DoublePoint field = new DoublePoint ("east", east);
        doc.add(field);

        field = new DoublePoint("north", north);
        doc.add(field);
      }




    }
  }

  static void temporal(File file, Document doc, String etiqueta){
    DocumentBuilderFactory dcf = DocumentBuilderFactory.newDefaultInstance();
    DocumentBuilder dcb;
    try {dcb = dcf.newDocumentBuilder();}
    catch (ParserConfigurationException e) {throw new RuntimeException(e);}

    org.w3c.dom.Document dom;
    try { dom = dcb.parse(file);}
    catch (SAXException | IOException e) {throw new RuntimeException(e);}

    NodeList listNodes = dom.getElementsByTagName("dcterms:" + etiqueta);

    //System.out.println();
    //para cada lista recorro, para cada nodo coger contenido guardar el el doc
    for(int i = 0; i < listNodes.getLength(); i++){

      //System.out.println(listNodes.item(i).getNodeName());
      Node nodo = listNodes.item(i);
      //System.out.println(nodo.getTextContent());
      String result = nodo.getTextContent().replaceAll("-", "");
      Field aux = new StringField(etiqueta,result,Field.Store.YES);
      System.out.println(result);
      doc.add(aux);
    }
  }

  /**
   * Indexes the given file using the given writer, or if a directory is given,
   * recurses over files and directories found under the given directory.
   *
   * NOTE: This method indexes one document per input file.  This is slow.  For good
   * throughput, put multiple documents into your input file(s).  An example of this is
   * in the benchmark module, which can create "line doc" files, one document per line,
   * using the
   * <a href="../../../../../contrib-benchmark/org/apache/lucene/benchmark/byTask/tasks/WriteLineDocTask.html"
   * >WriteLineDocTask</a>.
   *
   * @param writer Writer to the index where the given file/dir info will be stored
   * @param file The file to index, or the directory to recurse into to find files to index
   * @throws IOException If there is a low-level I/O error
   */
  static void indexDocs(IndexWriter writer, File file)
          throws IOException {
    // do not try to index files that cannot be read
    if (file.canRead()) {
      if (file.isDirectory()) {
        String[] files = file.list();
        // an IO error could occur
        if (files != null) {
          for (int i = 0; i < files.length; i++) {
            indexDocs(writer, new File(file, files[i]));
          }
        }
      } else {

        FileInputStream fis;
        try {
          fis = new FileInputStream(file);
        } catch (FileNotFoundException fnfe) {
          // at least on windows, some temporary files raise this exception with an "access denied" message
          // checking if the file can be read doesn't help
          return;
        }

        try {

          // make a new, empty document
          Document doc = new Document();

          // Add the path of the file as a field named "path".  Use a
          // field that is indexed (i.e. searchable), but don't tokenize
          // the field into separate words and don't index term frequency
          // or positional information:
          Field pathField = new StringField("path", file.getPath(), Field.Store.YES);
          doc.add(pathField);

          // Add the last modified date of the file a field named "modified".
          // Use a StoredField to return later its value as a response to a query.
          // This indexes to milli-second resolution, which
          // is often too fine.  You could instead create a number based on
          // year/month/day/hour/minutes/seconds, down the resolution you require.
          // For example the long value 2011021714 would mean
          // February 17, 2011, 2-3 PM.
          doc.add(new StoredField("modified", file.lastModified()));

          // Add the contents of the file to a field named "contents".  Specify a Reader,
          // so that the text of the file is tokenized and indexed, but not stored.
          // Note that FileReader expects the file to be in UTF-8 encoding.
          // If that's not the case searching for special characters will fail.
          doc.add(new TextField("contents", new BufferedReader(new InputStreamReader(fis, "UTF-8"))));

          //Text field
          //type, creator, contributor, publisher, title, description, date, subject
          type(file, doc, "type");
          ajustadoText(file, doc, "creator");
          ajustadoText(file, doc, "contributor");
          ajustadoText(file, doc, "publisher");
          ajustadoText(file, doc, "title");
          ajustadoText(file, doc, "description");
          ajustadoText(file, doc, "date");
          ajustadoText(file, doc, "subject");

          //String field
          //identifier, language, relation, rights
          ajustadoString(file, doc, "identifier");
          ajustadoString(file, doc, "language");
          //ajustadoString(file, doc, "format");
          ajustadoString(file, doc, "relation");
          ajustadoString(file, doc, "rights");

          coordenadas(file, doc, "LowerCorner");
          coordenadas(file, doc, "UpperCorner");

          temporal(file, doc, "issued");
          temporal(file, doc, "created");



          if (writer.getConfig().getOpenMode() == OpenMode.CREATE) {
            // New index, so we just add the document (no old document can be there):
            //System.out.println("adding " + file);
            writer.addDocument(doc);
          } else {
            // Existing index (an old copy of this document may have been indexed) so 
            // we use updateDocument instead to replace the old one matching the exact 
            // path, if present:
            System.out.println("updating " + file);
            writer.updateDocument(new Term("path", file.getPath()), doc);
          }

        } finally {
          fis.close();
        }
      }
    }
  }
}