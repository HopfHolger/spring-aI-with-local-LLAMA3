# Spring AI with local LLAMA3

## Overview
Der Anfang dieses Projketes war ein git-Projekt https://github.com/gdorsi44/spring-aI-with-local-LLAMA3 welches ich dann erweitert habe.
"This demo demonstrates the integration of a local LLama3 model for handling JSON responses within the Spring AI framework, positioning the model as a 'business logic' executor. This provides a starting point specifically tailored for simple artificial intelligence workflows, using Java and Spring AI."


## Prerequisites
- Java 17/21
- Maven 3.3+ for building and managing the project

## Technologies
- **Spring Boot**: Simplifies the bootstrapping and development of new Spring Framework applications.
- **Spring AI**: Provides AI capabilities integrated seamlessly with Spring applications. In this project, we use local LLM `spring-ai-ollama-spring-boot-starter` for integrating the LLama3 llm.
- **Ollama**: a tool to run Large Language Models locally for manage safe private data.

## Getting Started

Ziel eine funktionierende Kette:Java/Spring AI \(\rightarrow \) Ollama (mxbai-embed-large) \(\rightarrow \) PostgreSQL (pgvector). 

1. **Install and run LLama3 with Ollama**:
    ```bash
    ollama run llama3 
    ollama pull mistral // ein Modell
    ollama pull mxbai-embed-large
    ollama rm llama3
    ollama list oder ls
   
   Falls du ein spezielles Modell für Embeddings (Vektorisierung) nutzt (was oft performanter ist), solltest du auch dieses laden, falls es in deiner application.properties steht.
2. **Clone the Repository**:
   ```bash
   git clone https://your-repository-url-here
   cd search-book-with-llm

3. **Build the Application**:
   ```bash
   mvn clean install

4. **Run the Application**:
   ```bash
   mvn spring-boot:run
   
5. **Access the postgress mit PostgreSQL + pgvector **:
   ```bash
   docker-compose up -d

Pro-Tipp: Wenn du Ressourcen sparen willst, lerne das Modell mxbai-embed-large kennen (mit ollama pull mxbai-embed-large). Es ist speziell für Embeddings optimiert und nutzt oft nur 1024 Dimensionen, was deine Datenbank-Indizes deutlich schneller macht als das riesige Mistral-Modell. Wir nehmen llama3.1, wei les Tool-Calling kann.
Das ist ein kritischer Punkt: Wenn die Dimensionen deines Embedding-Modells nicht exakt mit der Spaltendefinition in deiner PostgreSQL übereinstimmen, wird die Datenbank jeden Schreib- oder Suchversuch mit einem Fehler abbrechen.


1. Die VektorDimensionen deiner KI prüfen:
    Jedes Modell erzeugt Vektoren mit einer festen Länge. Hier sind die gängigsten Werte:
   OpenAI text-embedding-3-small (Standard): 1536 Dimensionen.
   OpenAI text-embedding-3-large: 3072 Dimensionen.
   Ollama mit mxbai-embed-large: 1024 Dimensionen.
   HuggingFace all-MiniLM-L6-v2: 384 Dimensionen.

   Was bedeutet "1536 Dimensionen"?
    Modelle wie OpenAIs text-embedding-ada-002 oder text-embedding-3-small wandeln Wörter, Sätze oder ganze Absätze in eine Liste von exakt 1536 Zahlen (Fließkommawerten) um.
   Der Vektor: Diese Liste von Zahlen nennt man einen „Vektor“.
   Der Raum: Man kann sich das so vorstellen, dass jeder Text als ein Punkt in einem riesigen, 1536-dimensionalen Koordinatensystem platziert wird.
   Die Bedeutung: Jede dieser 1536 Dimensionen steht theoretisch für ein bestimmtes abstraktes Merkmal oder Konzept. Ein Text über „Hunde“ landet in diesem Raum näher bei „Welpen“ als bei „Backofen“, weil ihre Zahlenwerte in diesen 1536 Dimensionen ähnlicher sind.
   
   Warum genau diese Zahl?
   Standardisierung: Die feste Größe von 1536 ermöglicht es, jeden beliebigen Text – egal ob ein Wort oder eine Seite – mathematisch direkt miteinander zu vergleichen.
   Effizienz: 1536 ist ein Kompromiss. Höhere Dimensionen (wie 3072 bei neueren Modellen) erfassen feinere Nuancen, brauchen aber mehr Speicher und Rechenpower.
   Mathematische Notwendigkeit: Um Ähnlichkeiten zwischen Texten zu berechnen (z. B. für Suchmaschinen oder Chatbots), müssen die Vektoren die gleiche Länge haben.
   Anwendungsbeispiel:
   Wenn du eine Vektordatenbank aufbaust, musst du ihr oft vorher sagen, wie viele Dimensionen sie speichern soll (z. B. 1536), damit sie weiß, wie viel Platz sie für jeden Eintrag reservieren muss. 

In der Welt der KI und Vektordatenbanken ist die Cosine Distance (Kosinus-Distanz) das Standardmaß, um festzustellen, wie „nah“ sich zwei Texte inhaltlich sind.


