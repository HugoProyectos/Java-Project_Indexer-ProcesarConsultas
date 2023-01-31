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

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Objects;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.es.SpanishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/** Simple command-line based search demo. */
public class SearchFiles {

  private SearchFiles() {}


  static NodeList leerConsultas(File file, String etiqueta){
    DocumentBuilderFactory dcf = DocumentBuilderFactory.newDefaultInstance();
    DocumentBuilder dcb;
    try {dcb = dcf.newDocumentBuilder();}
    catch (ParserConfigurationException e) {throw new RuntimeException(e);}

    org.w3c.dom.Document dom;
    try { dom = dcb.parse(file);}
    catch (SAXException | IOException e) {throw new RuntimeException(e);}

    NodeList consultas = dom.getElementsByTagName(etiqueta);

    return consultas;
    //System.out.println();
    //para cada lista recorro, para cada nodo coger contenido guardar el el doc
    /*for(int i = 0; i < consultas.getLength(); i++){

      //System.out.println(listNodes.item(i).getNodeName());
      Node nodo = consultas.item(i);
      Node nodo2 = identificadores.item(i);
      System.out.println(nodo2.getTextContent());
      System.out.println(nodo.getTextContent());
    }*/
  }


  /** Simple command-line based search demo. */
  public static void main(String[] args) throws Exception {
    String usage =
            "Usage:\tjava org.apache.lucene.demo.SearchFiles [-index dir] [-field f] [-repeat n] [-queries file] [-query string] [-raw] [-paging hitsPerPage]\n\nSee http://lucene.apache.org/core/4_1_0/demo/ for details.";
    if (args.length > 0 && ("-h".equals(args[0]) || "-help".equals(args[0]))) {
      System.out.println(usage);
      System.exit(0);
    }


    String index = "index";
    String field = "contents";
    String queries = null;
    String output = "";
    FileWriter archivo = null;
    int repeat = 0;
    boolean raw = false;
    String queryString = null;
    String infoNeed = null;
    int hitsPerPage = 10;

    for(int i = 0;i < args.length;i++) {
      if ("-index".equals(args[i])) {
        index = args[i+1];
        i++;
      } else if ("-field".equals(args[i])) {
        field = args[i+1];
        i++;
      } else if ("-queries".equals(args[i])) {
        queries = args[i+1];
        i++;
      } else if ("-query".equals(args[i])) {
        queryString = args[i+1];
        i++;
      } else if ("-infoNeeds".equals(args[i])) {
        infoNeed = args[i+1];
        i++;
      } else if ("-repeat".equals(args[i])) {
        repeat = Integer.parseInt(args[i+1]);
        i++;
      } else if ("-output".equals(args[i])) {
        output = args[i+1];
        archivo = new FileWriter(output);
        i++;
      } else if ("-raw".equals(args[i])) {
        raw = true;
      } else if ("-paging".equals(args[i])) {
        hitsPerPage = Integer.parseInt(args[i+1]);
        if (hitsPerPage <= 0) {
          System.err.println("There must be at least 1 hit per page.");
          System.exit(1);
        }
        i++;
      }
    }

    IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(index)));
    IndexSearcher searcher = new IndexSearcher(reader);
    Analyzer analyzer = new SpanishAnalyzer2();
    //Analyzer analyzer = new WhitespaceAnalyzer();

    BufferedReader in = null;
    if (queries != null) {
      in = new BufferedReader(new InputStreamReader(new FileInputStream(queries), "UTF-8"));
    } else {
      in = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
    }

    BufferedReader fileReader = null;
    NodeList consultas = null;
    NodeList identificadores = null;
    if(queryString != null){
      fileReader = new BufferedReader(new FileReader(queryString));
    }
    if(infoNeed != null){
      File file = new File(infoNeed);
      consultas = leerConsultas(file, "text");
      identificadores = leerConsultas(file, "identifier");
      /*
      for(int i = 0; i < consultas.getLength(); i++){

        //System.out.println(listNodes.item(i).getNodeName());
        Node nodo = consultas.item(i);
        Node nodo2 = identificadores.item(i);
        System.out.println(nodo2.getTextContent());
        System.out.println(nodo.getTextContent());
      }

      */
    }

    QueryParser parser = new QueryParser(field, analyzer);
    int consulta = 0;
    while (true) {
      if (queries == null && queryString == null && infoNeed == null) {                        // prompt the user
        System.out.println("Enter query: ");
      }

      if(consultas!= null && consulta == consultas.getLength()){
        break;
      }

      String line = null;
      if(queryString == null && infoNeed == null) {
        line = in.readLine();
      } else if(infoNeed == null){
        line = fileReader.readLine();
        consulta = consulta + 1;
      } else{

        Node nodo = consultas.item(consulta);

        try (InputStream modelIn = new FileInputStream("opennlp-es-pos-maxent-pos-es.model")){
          POSModel model = new POSModel(modelIn);
          POSTaggerME tagger = new POSTaggerME(model);
          String necesidad = nodo.getTextContent();
          //Eliminar los comentarios entre paréntesis para la consulta
          necesidad = necesidad.replaceAll("\\(.*\\)", "");
          String sent[] = necesidad.split(" ");
          String tags[] = tagger.tag(sent);

          line = "";
          int i = 0;
          for (String tag : tags){
            //Coger nombres, numeros
            //line = line + tag + " ";
            if(Objects.equals(tag, "NC") || Objects.equals(tag, "Z") || Objects.equals(tag, "AQ")){
              line = line + sent[i] + " ";
            }
            i++;
          }
          System.out.println(nodo.getTextContent());

        }
        consulta = consulta + 1;
      }

      if (line == null || line.length() == -1) {
        break;
      }

      // spatial:<west>,<east>,<south>,<north>
      BooleanQuery booleanQuery = null;
      //System.out.println(line);
      String partes[] = line.split(" ");
      String spatial = "";
      if(partes[0].split(":")[0].equals("spatial")){
        spatial = partes[0];
        line = "";
        for(int i = 1; i < partes.length; i++){
          line += partes[i];
        }
      }
      System.out.println("Line: " + line);
      System.out.println("Spatial: " + spatial);

      if (spatial.length() != 0) {
        String[] coords = spatial.split(":")[1].split(",");
        Double west = Double.parseDouble(coords[0]);
        Double east = Double.parseDouble(coords[1]);
        Double south = Double.parseDouble(coords[2]);
        Double north = Double.parseDouble(coords[3]);

        //Xmax ≥ West
        Query westRangeQuery = DoublePoint.newRangeQuery("west" , west, Double.POSITIVE_INFINITY);

        //Xmin ≤ East
        Query eastRangeQuery = DoublePoint.newRangeQuery("east" , Double.NEGATIVE_INFINITY, east);

        //Ymax ≥ South
        Query southRangeQuery = DoublePoint.newRangeQuery("south" , south,  Double.POSITIVE_INFINITY);

        //Ymin ≤ North
        Query northRangeQuery = DoublePoint.newRangeQuery("north" , Double.NEGATIVE_INFINITY, north);

        System.out.println("Coordenadas");
        booleanQuery = new BooleanQuery.Builder().add(westRangeQuery, BooleanClause.Occur.MUST).add(eastRangeQuery, BooleanClause.Occur.MUST).add(southRangeQuery, BooleanClause.Occur.MUST).add(northRangeQuery, BooleanClause.Occur.MUST).build();
      }

      if(line.length() != 0){
        line = line.trim();
      }

      if (line.length() == 0 && spatial.length() == 0) {
        break;
      }
      //Buscando los documentos donde este line
      //System.out.println("Searching for: " + booleanQuery.toString(field));
      Query query = null;
      Query lineQuery = null;
      if(line.length() != 0){
        lineQuery = parser.parse(line);
        query = lineQuery;
      }


      //Añadir la lineQuery a la booleanQuery
      if(spatial.length() != 0 && line.length() != 0) {
        booleanQuery = new BooleanQuery.Builder().add(booleanQuery, BooleanClause.Occur.SHOULD).add(lineQuery, BooleanClause.Occur.SHOULD).build();
        query = booleanQuery;
        System.out.println("Searching for: " + query.toString(field));
      } else if(spatial.length() != 0){
        query = booleanQuery;
        System.out.println("Searching for: " + query.toString(field));
      }

      if (repeat > 0) {                           // repeat & time as benchmark
        Date start = new Date();
        for (int i = 0; i < repeat; i++) {
          searcher.search(query, 100);
        }
        Date end = new Date();
        System.out.println("Time: "+(end.getTime()-start.getTime())+"ms");
      }

      doPagingSearch(in, searcher, query, hitsPerPage, output, raw, queries == null && queryString == null && infoNeed == null, archivo, consulta, identificadores);

    }

    reader.close();
    if(!output.equals("")) {
      archivo.close();
    }

  }

  /**
   * This demonstrates a typical paging search scenario, where the search engine presents
   * pages of size n to the user. The user can then go to the next page if interested in
   * the next hits.
   *
   * When the query is executed for the first time, then only enough results are collected
   * to fill 5 result pages. If the user wants to page beyond this limit, then the query
   * is executed another time and all hits are collected.
   *
   */
  public static void doPagingSearch(BufferedReader in, IndexSearcher searcher, Query query,
                                    int hitsPerPage, String output, boolean raw, boolean interactive, FileWriter archivo, int consulta, NodeList identificadores) throws IOException {

    // Collect enough docs to show 5 pages
    TopDocs results = searcher.search(query, 5 * hitsPerPage);
    ScoreDoc[] hits = results.scoreDocs;

    int numTotalHits = Math.toIntExact(results.totalHits.value);
    System.out.println(numTotalHits + " total matching documents");

    int start = 0;
    int end = 0;
    if(!output.isEmpty()){
      end = hitsPerPage;
    }else{
      end = Math.min(numTotalHits, hitsPerPage);
    }



    while (true) {
      if (end > hits.length && output == null) {
        System.out.println("Only results 1 - " + hits.length +" of " + numTotalHits + " total matching documents collected.");
        System.out.println("Collect more (y/n) ?");
        String line = in.readLine();
        if (line.length() == 0 || line.charAt(0) == 'n') {
          break;
        }

        hits = searcher.search(query, numTotalHits).scoreDocs;
      }
      end = Math.min(hits.length, start + hitsPerPage);

      for (int i = start; i < end; i++) {
        //System . out . println ( searcher . explain ( query , hits [ i ]. doc ) ) ;
        if (raw) {                              // output raw format
          System.out.println("doc="+hits[i].doc +" score="+hits[i].score);
          continue;
        }
        if(!output.isEmpty()) {
          int j = 0;
          hits = searcher.search(query, numTotalHits).scoreDocs;
          while(j < hits.length){
            Document doc = searcher.doc(hits[j].doc);
            String name = doc.get("path");
            Path p = Paths.get(name);
            Node nodo = identificadores.item(consulta-1);
            archivo.append(nodo.getTextContent() + "\t" + p.getFileName().toString() + "\n");
            j++;
          }
          break;
        }

        else {
          // formato last modified: Tue Oct 08 23:39:50 CEST 2013
          /*
          Document doc = searcher.doc(hits[i].doc);
          File fis = new File(doc.get("path"));
          Date d = new Date(fis.lastModified());
          System.out.println("last modified: " + d);
          */

        }

        Document doc = searcher.doc(hits[i].doc);
        String path = doc.get("path");
        if (path != null) {
          System.out.println((i+1) + ". " + path);
        } else {
          System.out.println((i+1) + ". " + "No path for this document");
        }

      }

      if (!interactive || end == 0) {
        break;
      }

      if (numTotalHits >= end) {
        boolean quit = false;
        while (true) {
          System.out.print("Press ");
          if (start - hitsPerPage >= 0) {
            System.out.print("(p)revious page, ");
          }
          if (start + hitsPerPage < numTotalHits) {
            System.out.print("(n)ext page, ");
          }
          System.out.println("(q)uit or enter number to jump to a page.");

          String line = in.readLine();
          if (line.length() == 0 || line.charAt(0)=='q') {
            quit = true;
            break;
          }
          if (line.charAt(0) == 'p') {
            start = Math.max(0, start - hitsPerPage);
            break;
          } else if (line.charAt(0) == 'n') {
            if (start + hitsPerPage < numTotalHits) {
              start+=hitsPerPage;
            }
            break;
          } else {
            int page = Integer.parseInt(line);
            if ((page - 1) * hitsPerPage < numTotalHits) {
              start = (page - 1) * hitsPerPage;
              break;
            } else {
              System.out.println("No such page");
            }
          }
        }
        if (quit) break;
        end = Math.min(numTotalHits, start + hitsPerPage);
      }
    }
  }
}