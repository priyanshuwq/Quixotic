---
configs:
- config_name: indiccorp_v2
  data_files:
  - split: asm_Beng
    path: "data/as.txt"
  - split: ben_Beng
    path: "data/bn.txt"
  - split: brx_Deva
    path: "data/bd.txt"
  - split: doi_Deva
    path: "data/dg.txt"
  - split: gom_Deva
    path: "data/gom.txt"
  - split: guj_Gujr
    path: "data/gu.txt"
  - split: hin_Deva
    path: "data/hi-*.txt"
  - split: kan_Knda
    path: "data/kn.txt"
  - split: kas_Arab
    path: "data/ks.txt"
  - split: mai_Deva
    path: "data/mai.txt"
  - split: mal_Mlym
    path: "data/ml.txt"
  - split: mar_Deva
    path: "data/mr.txt"
  - split: mni_Mtei
    path: "data/mni.txt"
  - split: npi_Deva
    path: "data/ne.txt"
  - split: ory_Orya
    path: "data/or.txt"
  - split: pan_Guru
    path: "data/pa.txt"
  - split: san_Deva
    path: "data/sa.txt"
  - split: snd_Deva
    path: "data/sd.txt"
  - split: tam_Taml
    path: "data/ta.txt"
  - split: tel_Telu
    path: "data/te.txt"
  - split: urd_Arab
    path: "data/ur.txt"
  - split: khasi
    path: "data/kha.txt"
  - split: santhali
    path: "data/sat.txt"
---
# IndicCorp v2 Dataset

## Towards Leaving No Indic Language Behind: Building Monolingual Corpora, Benchmark and Models for Indic Languages
> This repository contains the pretraining data for the paper published at ACL 2023.

# Example Usage
```python
from datasets import load_dataset

# Load the Telugu subset of the dataset
dataset = load_dataset("ai4bharat/IndicCorpV2", "indiccorp_v2", data_dir="data/tel_Telu")
```


# License
All the datasets created as part of this work will be released under a [CC-0](https://creativecommons.org/publicdomain/zero/1.0) license and all models & code will be release under an [MIT license](https://github.com/ai4bharat/IndicBERT/blob/main/LICENSE)


# Citation
```bibtex
@inproceedings{doddapaneni-etal-2023-towards,
    title = "Towards Leaving No {I}ndic Language Behind: Building Monolingual Corpora, Benchmark and Models for {I}ndic Languages",
    author = "Doddapaneni, Sumanth  and
      Aralikatte, Rahul  and
      Ramesh, Gowtham  and
      Goyal, Shreya  and
      Khapra, Mitesh M.  and
      Kunchukuttan, Anoop  and
      Kumar, Pratyush",
    editor = "Rogers, Anna  and
      Boyd-Graber, Jordan  and
      Okazaki, Naoaki",
    booktitle = "Proceedings of the 61st Annual Meeting of the Association for Computational Linguistics (Volume 1: Long Papers)",
    month = jul,
    year = "2023",
    address = "Toronto, Canada",
    publisher = "Association for Computational Linguistics",
    url = "https://aclanthology.org/2023.acl-long.693",
    doi = "10.18653/v1/2023.acl-long.693",
    pages = "12402--12426",
    abstract = "Building Natural Language Understanding (NLU) capabilities for Indic languages, which have a collective speaker base of more than one billion speakers is absolutely crucial. In this work, we aim to improve the NLU capabilities of Indic languages by making contributions along 3 important axes (i) monolingual corpora (ii) NLU testsets (iii) multilingual LLMs focusing on Indic languages. Specifically, we curate the largest monolingual corpora, IndicCorp, with 20.9B tokens covering 24 languages from 4 language families - a 2.3x increase over prior work, while supporting 12 additional languages. Next, we create a human-supervised benchmark, IndicXTREME, consisting of nine diverse NLU tasks covering 20 languages. Across languages and tasks, IndicXTREME contains a total of 105 evaluation sets, of which 52 are new contributions to the literature. To the best of our knowledge, this is the first effort towards creating a standard benchmark for Indic languages that aims to test the multilingual zero-shot capabilities of pretrained language models. Finally, we train IndicBERT v2, a state-of-the-art model supporting all the languages. Averaged across languages and tasks, the model achieves an absolute improvement of 2 points over a strong baseline. The data and models are available at \url{https://github.com/AI4Bharat/IndicBERT}.",
}
```