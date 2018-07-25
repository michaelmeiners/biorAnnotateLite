# BioR Annotate Lite (bior_annotate_lite)
**A very simple implementation of bior-annotate that runs interactively and uses the BioR toolkit commands**

# Setup
1) Setup BioR, Bgzip, and biorAnnotateLite
Choose the latest version of BioR.  For example:
> export BIOR_LITE_HOME=/usr/local/biotools/bior_scripts/4.3.1/bior_pipeline-4.3.1

Choose location of bgzip
> export BGZIP_DIR=/data5/bsi/bictools/src/htslib/1.4/bin

Choose location of this biorAnnotateLite script
> export BIOR_ANNOTATE_LITE=/data5/bsi/BIOR/BiorAnnotateLite/1.0.0

Now, add all these to the path
> export PATH=$BIOR_LITE_HOME/bin:$BGZIP_DIR:$BIOR_ANNOTATE_LITE:$PATH



2) Run a test which shows how to run the script against a particular VCF and Catalog-Drill file
> $BIOR_ANNOTATE_LITE/Test/test.sh

OR, run it directly:
> bior_annotate_lite  my.in.vcf catalogDrill.txt  my.out.vcf.bgz  my.log



3) Now, try your own!

```
#-----------------------------------------------------------
# Working with a sample VCF 
#-----------------------------------------------------------
# First two lines in omim:
1       861117  879955  {"CytoLocation":["1p36.33"],"Confidence":"P","GeneName":"Sterile alpha motif domain-containing protein 11","MIMNumber":"616765","MappingMethod":["REc"],"LastUpdated":"21/01/2016","ComputedCytoLocation":"1p36.33","ApprovedSymbol":"SAMD11","EntrezGeneID":"148398","EnsemblGeneID":"ENSG00000187634","_maxBP":879955,"_landmark":"1","_minBP":861117,"BuildComment":"ApprovedSymbol","Transcript":"ENST00000342066","_strand":"+","ConfidenceDesc":"provisional","AbbrvTerms":["SAMD11","MRS"]}
1       879583  894670  {"CytoLocation":["1p36.33"],"Confidence":"P","GeneName":"Nucleolar complex-associated protein 2, S. cerevisiae, homolog of","MIMNumber":"610770","MappingMethod":["REc"],"LastUpdated":"20/02/2007","ComputedCytoLocation":"1p36.33","ApprovedSymbol":"NOC2L","EntrezGeneID":"26155","EnsemblGeneID":"ENSG00000188976","_maxBP":894670,"_landmark":"1","_minBP":879583,"BuildComment":"ApprovedSymbol","Transcript":"ENST00000327044","_strand":"-","ConfidenceDesc":"provisional","AbbrvTerms":["NOC2L","NIR"]}


# Two lines from dbSNP that fall in these ranges:
1	861119	861119	{"CHROM":"1","POS":861119,"ID":["rs925345761"],"REF":"C","ALT":"T","INFO":{"RS":["925345761"],"RSPOS":[861119],"dbSNPBuildID":[150],"SSR":[0],"SAO":[0],"VP":["0x0500000a0005000002000100"],"GENEINFO":["LOC101928801:101928801","SAMD11:148398"],"WGT":[1],"VC":["SNV"],"INT":true,"R5":true,"ASP":true,"TOPMED":[0.999966,3.43454E-5]},"_id":"rs925345761","_type":"variant","_landmark":"1","_refAllele":"C","_altAlleles":["T"],"_minBP":861119,"_maxBP":861119}
1	879583	879583	{"CHROM":"1","POS":879583,"ID":["rs754622280"],"REF":"T","ALT":"A","INFO":{"RS":["754622280"],"RSPOS":[879583],"dbSNPBuildID":[144],"SSR":[0],"SAO":[0],"VP":["0x050000840305040002000100"],"GENEINFO":["SAMD11:148398","NOC2L:26155"],"WGT":[1],"VC":["SNV"],"REF":true,"SYN":true,"U3":true,"R3":true,"ASP":true,"VLD":true},"_id":"rs754622280","_type":"variant","_landmark":"1","_refAllele":"T","_altAlleles":["A"],"_minBP":879583,"_maxBP":879583}


# So, two lines for a test VCF that guarantees values in the columns, should be:
#CHROM	POS	ID	REF	ALT	QUAL	FILTER	INFO
1	861119	.	C	T	.	.	.
1	879583	.	T	A	.	.	.


#------------------------------------------------------
# Create a catalog-drill file (should be tab-delimited)
#------------------------------------------------------
cat catalogDrill.txt
#Operation      CatalogPath     DrillPaths
bior_same_variant       /data5/bsi/catalogs/bior/v1/dbSNP/150_GRCh37.p13/variants.v1/All_dbSNP.tsv.bgz  ID,RSPOS,dbSNPBuildId,GENEINFO
bior_overlap    /data5/bsi/catalogs/bior/v1/omim/20180104_GRCh37.p13/disease.v1/omim_genes.tsv.bgz      MIMNumber,ApprovedSymbol,EntrezGeneID,EnsemblGeneID,Transcript

```

#------------------------------------------------------
# Splitting a VCF into multiple chunks, annotating those chunks, then merging them back into one file
#------------------------------------------------------
cd Test2
./run.sh

