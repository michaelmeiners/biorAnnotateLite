//--------------------------------------------------------------------------------------------------
// Cut out the sample columns to reduce the memory and processing overload for bior_annotate.sh
// when running with VCFs with lot of sample columns (hundreds to thousands).  This must be used
// with the merge.groovy script to combine the sample and header info from the original VCF after
// annotation is finished.
// 
// This should be run after the vcf-split command is called which splits lines with multiple alts
// 
// Given a VCF, this:
//   - cuts out the FORMAT and all sample columns, truncating to 8 columns.
//   - replaces the INFO column with a dot.
//   - removes any headers that may cause problems (like ##FORMAT and ##SAMPLE)
//   - adds a "biorLineNum" key to the INFO column to help merge after annotation
//--------------------------------------------------------------------------------------------------

//import net.sf.samtools.util.BlockCompressedInputStream;
//import net.sf.samtools.util.BlockCompressedOutputStream;
import htsjdk.samtools.util.BlockCompressedInputStream;
import htsjdk.samtools.util.BlockCompressedOutputStream;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.text.DecimalFormat;

if( args.length != 3 ) {
  usage();
  System.exit(1);
}

File vcfInFile = new File(args[0]);
File outputDir = new File(args[1]);
Long linesPerChunk = Long.parseLong(args[2]);

// Process the file - removing the sample columns, etc
removeSampleColumns_splitAlts_chunkFile(vcfInFile, outputDir, linesPerChunk)


//----------------------------------------------------------------------------------------

//------------------------------------------------------
private void usage() {
  println("groovy cutSamples.groovy  <VCF_INPUT_PATH>  <OUTPUT_DIR>  <NUM_LINES_PER_CHUNK>")
  println("  VCF_INPUT_PATH file may be in plain-text, gzip, or bgzip format, but should have the appropriate extensions: .vcf, .gz, .bgz")
}

//------------------------------------------------------
private void removeSampleColumns_splitAlts_chunkFile(File vcfInFile, File outputDir, long numLinesPerChunk) {
  // VCF file can be plain-text, gzip, or bgzip
  BufferedReader vcfReader = getFileReader(vcfInFile)


  // VCF file out - can be plain-text (.vcf), gzip (.gz), or bgzip (.bgz) based on extension.
  OutputStream vcfWriter = null;
  int chunkFileNum = 0;

  long lineNumTotal = 0;
  long lineNumInChunk = 0;

  String line = null;
  StringBuilder header = new StringBuilder();
  while( (line = vcfReader.readLine()) != null ) {
    if( line.startsWith("##") )
      header.append(line + "\n");
    else if( line.startsWith("#") )
      header.append(getFirst5Cols(line) + "\t" + "QUAL" + "\t" + "FILTER" + "\t" + "INFO" + "\n");
    else {
      lineNumTotal++
      lineNumInChunk++

      if( lineNumTotal == 1  ||  lineNumInChunk > numLinesPerChunk ) {
        if( vcfWriter != null ) {
          vcfWriter.flush()
          vcfWriter.close()
        }
        File nextChunkFile = getNextChunkFile(vcfInFile, outputDir, chunkFileNum)
        vcfWriter = getFileWriter(nextChunkFile)
        vcfWriter.write(header.toString().getBytes())
        chunkFileNum++
        lineNumInChunk = 1
      }  

      //              FIRST_FIVE_COLS             QUAL         FILTER         INFO
      String cutLine = getFirst5Cols(line) + "\t" + "." + "\t" + "." + "\t" + "biorLineNum=" + lineNumTotal + "\n"
      vcfWriter.write(cutLine.getBytes())
      
      // TODO: Need to include alt-splitting here.......  (Ex:  Break up alts "A,C,G" into three lines, all with same biorLineNum)
    }
  }
  vcfReader.close()
  vcfWriter.close()
}

private File getNextChunkFile(File vcfInFile, File outputDir, long fileChunkNum) {
  DecimalFormat NUM_FORMAT = new DecimalFormat("000000");
  return new File(outputDir, vcfInFile.getName() + "." + NUM_FORMAT.format(fileChunkNum) + ".gz");
}

//------------------------------------------------------
private BufferedReader getFileReader(File vcfInFile) {
  if( vcfInFile.getName().toLowerCase().endsWith(".gz") || vcfInFile.getName().toLowerCase().endsWith(".bgz") ) {
    return new BufferedReader(new InputStreamReader(new BlockCompressedInputStream(new FileInputStream(vcfInFile))))
  } else {
    return new BufferedReader(new FileReader(vcfInFile))
  }
}

//------------------------------------------------------
private OutputStream getFileWriter(File vcfOutFile) {
  if( vcfOutFile.getName().toLowerCase().endsWith(".gz")  ||  vcfOutFile.getName().toLowerCase().endsWith(".bgz") ) {
    return new BlockCompressedOutputStream(vcfOutFile.getCanonicalPath(), 9);
  } else {
    return new BufferedWriter(new FileWriter(vcfOutFile));
  }
}

//------------------------------------------------------
// In a VCF, the first 5 cols are: "CHROM", "POS", "ID", "REF", "ALT"
private String getFirst5Cols(String s) {
  return getFirstXCols(s, 5)
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
