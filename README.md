# pedigree-reconstruction (PREPARE)

## Introduction
This repo contains four tools for pedigree-reconstruction:
1. pedigree_reconstruction - for reconstructing pedigrees from IBD data of extant population
2. simulate_pedigree - for simulating pedigree data
3. calculate_ibd_loss - for evaluating how good does a pedigree fits IBD data
4. compare_pedigrees - for comparing two pedigrees, usually simulated and predicted

## Run
~~~
python prepare.py <tool_name> <tool_args>
~~~
For the list of available tools:
~~~
python prepare.py
~~~

## Build
mvn install

## Reference
Historical Pedigree Reconstruction from Extant Populations Using PArtitioning of RElatives PREPARE: 
https://journals.plos.org/ploscompbiol/article?id=10.1371/journal.pcbi.1003610



