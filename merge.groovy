import htsjdk.samtools.util.BlockCompressedInputStream;
import htsjdk.samtools.util.BlockCompressedOutputStream;
import java.util.zip.GZIPInputStream;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

//runTestSuite()


if( args.length < 3 ) { 
  printUsage()
  System.exit(1)
}


File originalHugeVcfFile = new File(args[0])
File vcfOutputFile       = new File(args[1])
List<File> chunkFileList = new ArrayList<File>()
for(int i=2; i < args.length; i++) {
  chunkFileList.add(new File(args[i]))
}

Collections.sort(chunkFileList)

mergeChunkFilesWithOriginalHugeVcf(originalHugeVcfFile, vcfOutputFile, chunkFileList)


//------------------------------------------------------
private void printUsage() {
  println("USAGE:")
  println("  merge <ORIGINAL_HUGE_VCF>  <VCF_OUTPUT_FILE>  <CHUNK_FILES...>")
  println("  WHERE:")
  println("    - ORIGINAL_HUGE_VCF  is the original VCF file that may contain a huge number of sample columns")
  println("    - VCF_OUTPUT_FILE is the output file to write to that will combine the BioR annotations")
  println("      from annotatedVcf with the sample columns from originalHugeVcf (along with any other columns after column 8, such as FORMAT)")
  println("      The extension will determine if it is plain-text (.vcf) or zipped (.gz or .bgz)")
  println("    - CHUNK_FILES is a list of chunk files that will be combiled (Ex: use wildcards, like: test.00*.gz")
  println("      NOTE: These files will be sorted by filename before being combined!")
}


//------------------------------------------------------
private void mergeChunkFilesWithOriginalHugeVcf(File originalHugeVcfFile, File vcfOutputFile, List<File> chunkFileList) {
  // Get reader for original HUGE # of columns VCF
  BufferedReader originalHugeVcfReader = getBufferedReader(originalHugeVcfFile)

  // Get writer for output
  OutputStream outWriter = getOutputStream(vcfOutputFile)


  // NOTE: In the final version, we should add all header metadata lines ("##...") from both the original and annotation files
  //       into a HashSet and sort them by type.  
  // NOTE: The final splitter code should remove these lines are they will not be relevant until merging: ##SAMPLE, ##FORMAT, ##contig (??)
  // .............. 
  // NOTE: The final version of the splitter should add a "BiorLineId=1", etc to each line which we can use in the merge
  //       Instead of trying to have the first 7 columns match
  // For now, only add the header from the first annotated chunk that is read
  boolean isFirstChunkFile = true

  // Go through each line in annotatedVcf and match it with the line in the originalHugeVcf
  String lineAnnot = null
  
  // Scan until we get to the "#CHROM..." header line
  String lineOrig = getFirstNonMetadataLine(originalHugeVcfReader)
  
  // Write all metadata header lines
  writeMetadataHeaderLinesFromFirstChunk(outWriter, chunkFileList.get(0))
  // Write the header from the original VCF
  outWriter.write( (lineOrig + "\n").getBytes() )
  
  // Read the next line in the original VCF, which should be the data line
  while( (lineOrig = originalHugeVcfReader.readLine()).startsWith("#") ) {
    // Do nothing
  }
  long lineNumOrig = 1

  for( File chunkFile : chunkFileList ) {
    // Reader for annotated VCF chunks (just the 8 cols)
    BufferedReader chunkReader = getBufferedReader(chunkFile)
    println("Processing chunk file: " + chunkFile.getName())
    while( (lineAnnot = chunkReader.readLine()) != null ) {
      if( lineAnnot.startsWith("#") )
        continue

      //println("-----------------------------------")
      //println("Annotated line: " + lineAnnot)
      
      // If the current original file line is NOT the same as the annotated one, then grab the next original line
      while( ! isSameLine(lineAnnot, lineNumOrig) ) {
        lineOrig = originalHugeVcfReader.readLine()
  	    lineNumOrig++
        //println("lineOrig: " + lineOrig)
        if( lineOrig == null ) 
          throw new Exception("ERROR:  Encountered end of original VCF file before matching with all lines from chunks")
        //println(" original(" + lineNumOrig + "): " + lineOrig.substring(0, Math.min(100, lineOrig.length())))
      }

      // At this point the lines should be the same
      //println("-------")
      //println("annot: (" + getLineNumAnnot(lineAnnot) + ") " + lineAnnot.substring(0, Math.min(100,lineAnnot.length())))
      //println("orig:  (" + lineNumOrig                + ") " + lineOrig.substring(0, Math.min(100, lineOrig.length())))

      String mergedLine = mergeLine(lineAnnot, lineOrig)
      outWriter.write( (mergedLine + "\n").getBytes() )
    }
    chunkReader.close()
  }
  
  originalHugeVcfReader.close()
  outWriter.close()
}


//------------------------------------------------------
private void writeMetadataHeaderLinesFromFirstChunk(OutputStream outWriter, File chunkFile) {
  BufferedReader fin = getBufferedReader(chunkFile)
  String line = null
  while( (line = fin.readLine()) != null ) {
    if( line.startsWith("##") )
      outWriter.write( (line + "\n").getBytes() )
    else
      break
  }
  fin.close()
}

//------------------------------------------------------
private boolean isSameLine(String annotLine, long lineNumOrig) {
  if( annotLine.startsWith("#") )
    return false
    
  long lineNumAnnot = getLineNumAnnot(annotLine)
  //println("    original line #: " + lineNumOrig)
  //println("    annotated line#: " + lineNumAnnot)
  
  return lineNumAnnot == lineNumOrig
}

//------------------------------------------------------
private long getLineNumAnnot(String annotLine) {
  final String BIOR_LINE_NUM_STR = "biorLineNum="
  int idx = annotLine.indexOf(BIOR_LINE_NUM_STR)
  int idxEnd = annotLine.indexOf(";", idx)
  if( idxEnd == -1 )
    idxEnd   = annotLine.length()
  String lineNumAnnotStr = annotLine.substring(idx + BIOR_LINE_NUM_STR.length(), idxEnd)
  long lineNumAnnot = Long.parseLong(lineNumAnnotStr)
  return lineNumAnnot
}


//------------------------------------------------------
private String getFirstNonMetadataLine(BufferedReader fin) {
  String line = null;
  while( (line = fin.readLine()) != null  &&  line.startsWith("##") ) { }
  return line;
}
    
//------------------------------------------------------
private String getFirst7Cols(String s) {
  return getFirstXCols(s, 7)
}

//------------------------------------------------------
// Get first X columns as a single string
private String getFirstXCols(String s, int numCols) {
  int count = 0;
  int idx = s.indexOf("\t")
  while( idx != -1 ) {
    count++
    if( count == numCols ) 
      return s.substring(0, idx)
    idx = s.indexOf("\t", idx+1)
  }
  // Must be less than numCols columns, so just return whole string
  return s;  
}

//------------------------------------------------------
// Return the 1-based column.  If the column is not found, return ""
//   Ex:  "1 2 3 4", col=3 will return "3"
//   Ex:  "1 2 3 4", col=5 will return ""
private String getCol(String s, int col) {
  int currentCol = 1
  int idxStart = 0
  int idxEnd   = getNextTabIdxOrEnd(s, 0) 
  while( currentCol < col  &&  idxEnd != s.length() ) {
    currentCol++
    idxStart = idxEnd + 1
    idxEnd = getNextTabIdxOrEnd(s, idxStart)
  }
  
  // If the correct column was found, then return it
  if( currentCol == col )
    return s.substring(idxStart, idxEnd)
    
  // Not found, so return ""
  return "";
}

private int getNextTabIdxOrEnd(String s, int start) {
  int idx = s.indexOf("\t", start)
  if( idx == -1 ) 
    return s.length()
  return idx
}

//------------------------------------------------------
// Take the 8th col from lineAnnot, and insert it in as the 8th col in lineOrig (AFTER the original file's INFO column data)
private String mergeLine(String lineAnnot, String lineOrig) {
  String annotInfoCol = getCol(lineAnnot, 8)

  // If there was no annotated INFO column, then just return the full original line
  if( annotInfoCol.equals("") )
    return lineOrig
  
  String orig7Cols = getFirstXCols(lineOrig, 7)
  String orig8Cols = getFirstXCols(lineOrig, 8)
  int idxOrigCol9 = orig8Cols.length() + 1
  if( idxOrigCol9 > lineOrig.length() )
    idxOrigCol9 = lineOrig.length()
  String origCol8  = getCol(lineOrig, 8).trim()
  if(origCol8.equals("."))
    origCol8 = ""
  
  String annotCol8 = removeBiorLineNum(getCol(lineAnnot, 8))

  //println("lineOrig:  " + lineOrig)
  //println("lineAnnot: " + lineAnnot)

  String mergedFirst8Cols = (origCol8.length() == 0)  ?
    orig7Cols + "\t" + annotCol8 :
    orig8Cols + ";"  + annotCol8

  // Remove any trailing semicolons from INFO column
  if( mergedFirst8Cols.endsWith(";") ) {
    mergedFirst8Cols = mergedFirst8Cols.substring(0, mergedFirst8Cols.length()-1)
    //println("removed semicolon!!!!!!!!!!!!!!!!!!!!!!!!")
  }

  // Replace any double semicolons with single
  mergedFirst8Cols = mergedFirst8Cols.replace(";;", ";")
  
  String mergedAll = mergedFirst8Cols + "\t" + lineOrig.substring(idxOrigCol9)

  return mergedAll
}

//------------------------------------------------------
private String removeBiorLineNum(String infoCol) {
  final String BIOR_LINE_NUM_STR = "biorLineNum="
  int idxStart = infoCol.indexOf(BIOR_LINE_NUM_STR)
  int idxEnd   = infoCol.indexOf(";", idxStart)
  if( idxEnd == -1 )
    idxEnd = infoCol.length()
  else  // Include the semicolon to cut out
    idxEnd+=1
  return infoCol.substring(0, idxStart) + infoCol.substring(idxEnd)
}


//------------------------------------------------------
// Determine if file is plain-text, gz, or bzip (just from extension), then return appropriate BufferedReader
private BufferedReader getBufferedReader(File f) {
  if( f.getName().toLowerCase().endsWith(".bgz") ) {
    return new BufferedReader(new InputStreamReader(new BlockCompressedInputStream(f)));
  } else if( f.getName().toLowerCase().endsWith(".gz") ) {
	return new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(f))))
  } else {
    return new BufferedReader(new InputStreamReader(new FileInputStream(f)))
  }
}


//------------------------------------------------------
private OutputStream getOutputStream(File fileOut) {
  if( fileOut.getName().toLowerCase().endsWith(".gz")   ||  fileOut.getName().toLowerCase().endsWith(".bgz") )
    return  new BlockCompressedOutputStream(fileOut, 9)
  else
    return  new FileOutputStream(fileOut)
}


//=============================================================================================


private void runTestSuite() {
  testConcat()
  testCol()
  testFirst7Cols()
  testMergeLine()
  testSameLine()
  println("-------------------------------------")
  println("SUCCESS!  ALL TESTS PASSED!")
  println("-------------------------------------")
  System.exit(0)
}

private String testConcat() {
  assertEquals("1", concat("1"))
  assertEquals("1\t2", concat("1", "2"))
  assertEquals("1\t2\t3\t4\t55555", concat("1", "2", "3", "4", "55555"))
}

private String testCol() {
  assertEquals("a", getCol("a", 1))
  assertEquals("",  getCol("a", 0))
  assertEquals("",  getCol("a", 2))
  assertEquals("A", getCol("A\tB", 1))
  assertEquals("B", getCol("A\tB", 2))
  assertEquals("",  getCol("A\tB", 3))
  assertEquals("A", getCol("A\tB\tC\tD", 1))
  assertEquals("B", getCol("A\tB\tC\tD", 2))
  assertEquals("C", getCol("A\tB\tC\tD", 3))
  assertEquals("D", getCol("A\tB\tC\tD", 4))
  assertEquals("",  getCol("A\tB\tC\tD", 5))
}

private void testFirst7Cols() {
  assertEquals("1", getFirst7Cols("1"))
  assertEquals("##12345678", getFirst7Cols("##12345678"))
  assertEquals("1\t2\t3\t4\t5\t6\t7", getFirst7Cols("1\t2\t3\t4\t5\t6\t7"))
  assertEquals("1\t2\t3\t4\t5\t6\t7", getFirst7Cols("1\t2\t3\t4\t5\t6\t7\t8"))
  assertEquals("1\t2\t3\t4\t5\t6\t7", getFirst7Cols("1\t2\t3\t4\t5\t6\t7\t8\t9\t10\t11\t12"))
}

private void testMergeLine() {
  assertEquals("1\t2\t3\t4\5", mergeLine("##1", "1\t2\t3\t4\5"))

  assertEquals(concat("#CHROM", "POS", "ID", "REF", "ALT", "QUAL", "FILTER", "INFO", "FORMAT", "SAMPLE1"),
     mergeLine(concat("1",      "2",   "3",  "4",   "5",   "6",    "7",      "INFO", "9",      "10"),
               concat("#CHROM", "POS", "ID", "REF", "ALT", "QUAL", "FILTER", "XXXX", "FORMAT", "SAMPLE1")))

  assertEquals(concat("1", "100", "rs1", "A", "C", ".", ".", "AF=0.24;AC=3"),
     mergeLine(concat("a", "b",   "c",   "d", "e", "f", "g", "AF=0.24;AC=3"),
               concat("1", "100", "rs1", "A", "C", ".", ".", ".")))

  assertEquals(concat("1", "100", "rs1", "A", "C", ".", ".", "AF=0.24;AC=3", "format", "sample1", "sample2", "sample3", "sample4", "sample5"),
     mergeLine(concat("a", "b",   "c",   "d", "e", "f", "g", "AF=0.24;AC=3", "5",      ".|."),
               concat("1", "100", "rs1", "A", "C", ".", ".", ".",            "format", "sample1", "sample2", "sample3", "sample4", "sample5")))
}

private void testSameLine() {
  // Exact match
  assertTrue(isSameLine(
  		concat("1", "100", "rs1", "A", "C", "0.0", "vx=0", "AC=3"),
  		concat("1", "100", "rs1", "A", "C", "0.0", "vx=0", "AC=3") ))

  // Match, but with "chr" prefix, and differences in non-essential cols
  assertTrue(isSameLine(
  		concat("1",   "100", "rs1", "A", "C", "0.0",  "vx=0",  "AC=3"),
  		concat("chr1","100", "rs1", "A", "C", "0.00", "vx=0.0","AC=3.0") ))

  // Match, but with "chr" prefix, and ALTs subset
  assertTrue(isSameLine(
  		concat("1",   "100", "rs1", "A", "C",   "0.0",  "vx=0",  "AC=3"),
  		concat("chr1","100", "rs1", "A", "C,G", "0.00", "vx=0.0","AC=3.0") ))

  // NO Match, because ALT is not in the original ALTs set
  assertFalse(isSameLine(
  		concat("1",   "100", "rs1", "A", "A",   "0.0",  "vx=0",  "AC=3"),
  		concat("chr1","100", "rs1", "A", "C,G", "0.00", "vx=0.0","AC=3.0") ))
}

private String concat(String... s) {
  StringBuilder str = new StringBuilder()
  for(int i=0; i < s.length; i++) {
    if( i > 0 )
      str.append("\t")
    str.append(s[i])
  }
  return str.toString()
}
  
