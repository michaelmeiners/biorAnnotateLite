### Cut out sample columns and chunk the VCF into 3-line chunks
mkdir -p OutputDir
../cutOutVcfSamples_splitAlts_chunkFile.sh  test.vcf.bgz  OutputDir  3

### Annotate each chunk (normally this would be done in parallel)
for f in OutputDir/*.gz ; do  echo "Annotating: $f"; ../bior_annotate_lite  $f  catalog_drill.txt  $f.annot.bgz; done

### Merge the annotated files back into a single bgzip'd VCF
../merge.sh test.vcf.bgz  test.annotated.vcf.bgz  OutputDir/test.vcf.bgz.*.annot.bgz
