---
configs:
- config_name: default
  data_files:
    - split: test
      path: "data/train-00000-of-00001.parquet"
language:
- as
- bn
- brx
- doi
- en
- gom
- gu
- hi
- kn
- ks
- mai
- ml
- mr
- mni
- ne
- or
- pa
- sa
- sat
- sd
- ta
- te
- ur
language_details: >-
  asm_Beng, ben_Beng, brx_Deva, doi_Deva, eng_Latn, gom_Deva, guj_Gujr,
  hin_Deva, kan_Knda, kas_Arab, mai_Deva, mal_Mlym, mar_Deva, mni_Mtei, 
  npi_Deva, ory_Orya, pan_Guru, san_Deva, sat_Olck, snd_Deva, tam_Taml, 
  tel_Telu, urd_Arab
license: cc-by-4.0
language_creators:
- expert-generated
multilinguality:
- multilingual
- translation
pretty_name: in22-gen
size_categories:
- 1K<n<10K
task_categories:
- translation
---

# IN22-Gen

IN22 is a newly created comprehensive benchmark for evaluating machine translation performance in multi-domain, n-way parallel contexts across 22 Indic languages. IN22-Gen is a general-purpose multi-domain evaluation subset of IN22. It has been created from two sources: Wikipedia and Web Sources offering diverse content spanning news, entertainment, culture, legal, and India-centric topics. The evaluation subset consists of 1024 sentences translated across 22 Indic languages enabling evaluation of MT systems across 506 directions.

Here is the domain and source distribution of our IN22-Gen evaluation subset.

<table style="width: 40%">
    <tr>
        <td>domain</td>
        <td>web sources</td>
        <td>wikipedia</td>
    </tr>
    <tr>
        <td>culture</td>
        <td>40</td>
        <td>40</td>
    </tr>
    <tr>
        <td>economy</td>
        <td>40</td>
        <td>40</td>
    </tr>
    <tr>
        <td>education</td>
        <td>40</td>
        <td>40</td>
    </tr>
    <tr>
        <td>entertainment</td>
        <td>40</td>
        <td>40</td>
    </tr>
    <tr>
        <td>geography</td>
        <td>40</td>
        <td>40</td>
    </tr>
    <tr>
        <td>governments</td>
        <td>40</td>
        <td>40</td>
    </tr>
    <tr>
        <td>health</td>
        <td>40</td>
        <td>40</td>
    </tr>
    <tr>
        <td>industry</td>
        <td>40</td>
        <td>40</td>
    </tr>
    <tr>
        <td>legal</td>
        <td>40</td>
        <td>40</td>
    </tr>
    <tr>
        <td>news</td>
        <td>32</td>
        <td>32</td>
    </tr>
    <tr>
        <td>religion</td>
        <td>40</td>
        <td>40</td>
    </tr>
    <tr>
        <td>sports</td>
        <td>40</td>
        <td>40</td>
    </tr>
    <tr>
        <td>tourism</td>
        <td>40</td>
        <td>40</td>
    </tr>
    <tr>
        <td>total</td>
        <td>512</td>
        <td>512</td>
    </tr>
</table>

Please refer to the `Appendix E: Dataset Card` of the [preprint](https://arxiv.org/abs/2305.16307) on detailed description of dataset curation, annotation and quality control process.


### Dataset Structure

#### Dataset Fields

- `id`: Row number for the data entry, starting at 1.
- `context`: Context window of 3 sentences, typically includes one sentence before and after the candidate sentence.
- `source`: The source from which the candidate sentence is considered.
- `url`: The URL for the English article from which the sentence was extracted. Only available for candidate sentences sourced from Wikipedia
- `domain`: The domain of the sentence.
- `num_words`: The number of words in the candidate sentence.
- `bucket`: Classification of the candidate sentence as per predefined bucket categories.
- `sentence`: The full sentence in the specific language (may have _lang for pairings)

#### Data Instances

A sample from the `gen` split for the English language (`eng_Latn` config) is provided below. All configurations have the same structure, and all sentences are aligned across configurations and splits.

```python
{
   "id": 1,
   "context": "A uniform is often viewed as projecting a positive image of an organisation. Maintaining personal hygiene is also an important aspect of personal appearance and dressing. An appearance is a bunch of attributes related with the service person, like their shoes, clothes, tie, jewellery, hairstyle, make-up, watch, cosmetics, perfume, etc.",
   "source": "web",
   "url": "",
   "domain": "culture",
   "num_words": 24,
   "bucket": "18 - 25",
   "sentence": "An appearance is a bunch of attributes related to the service person, like their shoes, clothes, tie, jewellery, hairstyle, make-up, watch, cosmetics, perfume, etc."
}
```

When using a hyphenated pairing or using the `all` function, data will be presented as follows:

```python
{
   "id": 1,
   "context": "A uniform is often viewed as projecting a positive image of an organisation. Maintaining personal hygiene is also an important aspect of personal appearance and dressing. An appearance is a bunch of attributes related with the service person, like their shoes, clothes, tie, jewellery, hairstyle, make-up, watch, cosmetics, perfume, etc.",
   "source": "web",
   "url": "",
   "domain": "culture",
   "num_words": 24,
   "bucket": "18 - 25",
   "sentence_eng_Latn": "An appearance is a bunch of attributes related to the service person, like their shoes, clothes, tie, jewellery, hairstyle, make-up, watch, cosmetics, perfume, etc.",
   "sentence_hin_Deva": "सेवा संबंधी लोगों के लिए भेष कई गुणों का संयोजन है, जैसे कि उनके जूते, कपड़े, टाई, आभूषण, केश शैली, मेक-अप, घड़ी, कॉस्मेटिक, इत्र, आदि।"
}
```


### Usage Instructions

```python
from datasets import load_dataset

# download and load all the pairs
dataset = load_dataset("ai4bharat/IN22-Gen", "all")

# download and load specific pairs
dataset = load_dataset("ai4bharat/IN22-Gen", "eng_Latn-hin_Deva")
```

### Languages Covered

<table style="width: 40%">
    <tr>
        <td>Assamese (asm_Beng)</td>
        <td>Kashmiri (Arabic) (kas_Arab)</td>
        <td>Punjabi (pan_Guru)</td>
    </tr>
    <tr>
        <td>Bengali (ben_Beng)</td>
        <td>Kashmiri (Devanagari) (kas_Deva)</td>
        <td>Sanskrit (san_Deva)</td>
    </tr>
    <tr>
        <td>Bodo (brx_Deva)</td>
        <td>Maithili (mai_Deva)</td>
        <td>Santali (sat_Olck)</td>
    </tr>
    <tr>
        <td>Dogri (doi_Deva)</td>
        <td>Malayalam (mal_Mlym)</td>
        <td>Sindhi (Arabic) (snd_Arab)</td>
    </tr>
    <tr>
        <td>English (eng_Latn)</td>
        <td>Marathi (mar_Deva)</td>
        <td>Sindhi (Devanagari) (snd_Deva)</td>
    </tr>
    <tr>
        <td>Konkani (gom_Deva)</td>
        <td>Manipuri (Bengali) (mni_Beng)</td>
        <td>Tamil (tam_Taml)</td>
    </tr>
    <tr>
        <td>Gujarati (guj_Gujr)</td>
        <td>Manipuri (Meitei) (mni_Mtei)</td>
        <td>Telugu (tel_Telu)</td>
    </tr>
    <tr>
        <td>Hindi (hin_Deva)</td>
        <td>Nepali (npi_Deva)</td>
        <td>Urdu (urd_Arab)</td>
    </tr>
    <tr>
        <td>Kannada (kan_Knda)</td>
        <td>Odia (ory_Orya)</td>
    </tr>
</table>


### Citation

If you consider using our work then please cite using:

```
@article{gala2023indictrans,
title={IndicTrans2: Towards High-Quality and Accessible Machine Translation Models for all 22 Scheduled Indian Languages},
author={Jay Gala and Pranjal A Chitale and A K Raghavan and Varun Gumma and Sumanth Doddapaneni and Aswanth Kumar M and Janki Atul Nawale and Anupama Sujatha and Ratish Puduppully and Vivek Raghavan and Pratyush Kumar and Mitesh M Khapra and Raj Dabre and Anoop Kunchukuttan},
journal={Transactions on Machine Learning Research},
issn={2835-8856},
year={2023},
url={https://openreview.net/forum?id=vfT4YuzAYA},
note={}
}
```

