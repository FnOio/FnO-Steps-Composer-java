# FnO-Steps Workflow Composer (Java version)
Composing workflows dynamically using the [FnO-Steps](https://w3id.org/imec/ns/fno-steps#) vocabulary.

Based on the OSLO-steps workflow composer as presented in:

Dörthe Arndt, Sven Lieber, Raf Buyle, Sander Goossens, David De Block,
Ben De Meester, and Erik Mannens. 2021.
Dynamic Workflow Composition with OSLO-steps: Data Re-use and Simplification
of Automated Administration.
In Proceedings of the 11th Knowledge Capture Conference (K-CAP ’21),
December 2–3, 2021, Virtual Event, USA. ACM, New York, NY, USA,
4 pages.
<https://doi.org/10.1145/3460210.3493559>

Extensions / optimizations compared to original implementation:
- Only linked steps are taken into account to form a workflow;
- Output: explicit distinction between parallel or mutually exclusive steps;
- Output: added Mermaid state diagram and P-Plan as formats;
- Fast scalable shape matching for large workflows;
- Works together with recent EYE reasoning engine ([eyeling](https://github.com/eyereasoner/eyeling)), giving a performance boost.

## Getting started

### Prerequisites
- wc2j-\<version\>.jar, available on the [releases](https://github.com/FnOio/FnO-Steps-Composer-java/releases) page;
- Java runtime environment (JRE), version 25 or more recent;
- The eyeling source code, available on the [releases](https://github.com/eyereasoner/eyeling/releases) page. This is optional, but enables N3 reasoning.
- Node.js or Bun (tested) or other Javascript runtime (not tested) to run eyeling.

### Usage

In its simplest form, run it as:

```
java -jar wc2-<version>.jar -i <input directory>
```

This runs scenario's with data found in the input directory, without any reasoning.

There are more options. Running with `--help` gives:

```
 usage:  Workflow Composer [-c <arg>] [-e <arg>] [-h] -i <arg> [-o <arg>]
    [-r <arg>]

 Calculate next steps to take given context data.

          Options                               Description                
 -i, --input-dir <arg>          Directory containing following input files:
                                 shapes.ttl, states.ttl, steps.ttl,        
                                 goalStates.txt.                           
 -c, --context <arg>            Name of a file containing context data     
                                 (data to operate on).It must exist in the 
                                 input directory. If not given, all files  
                                 named `data_*` will be processed          
                                 alphabetically, as if playing a scenario. 
 -e, --eye-bin <arg>            Path to an EYE reasoner. Will be just 'eye'
                                 if omitted.                               
 -o, --output-dir <arg>         Path to output directory. If not given, the
                                 input directory will be used. A           
                                 subdirectory per context file is created. 
 -r, --reasoning-file <arg>     Name of an N3 file containing extra        
                                 reasoning on the current context. It must 
                                 be in the input directory.                
 -h, --help                     Print this help message.      
```

The `--eye-bin` parameter expects the path to `<eyeling-root>/bin/eyeling.cjs`. 

## Input directory & scenarios

The workflow composer typically runs a *scenario*.
A scenario consists of a set of *states*, *shapes* and *states*, a set of *data* files and a set of *goal states*.
Make sure all files are UTF-8 encoded.

Here is how a typical scenario directory looks like ([example](src/test/resources/scenarios/stepsWait)):
```
stepsWait/
├── data_01.ttl
├── data_02.ttl
├── data_03.ttl
├── data_04.ttl
├── goalStates.txt
├── knowledge.n3
├── shapes.ttl
├── states.ttl
└── steps.ttl
```
- `data_x.ttl`: The input data (context) at stage `x`. Every file represents an iteration of the flow. This data is used to reason upon. Can be controlled with the `-c` parameter
- `goalStates.txt`: A list of state IRIs that are a goal, i.e. end state of the flow. List one goal per line. These IRIs need to be found in `states.ttl`.
- `knowledge.n3`: Optional: N3 reasoning rules to apply on the context *before* generating a workflow. E.g. to model business logic. Can be controlled with `-r` parameter.
- `shapes.ttl`: The shapes that define a state. If the data complies to a shape, the flow is in the state that requires the shape.
- `states.ttl`: The possible states in this flow. A state lists required shapes.
- `steps.ttl`: The possible steps in this flow. A step lists required states, and which state(s) it produces.