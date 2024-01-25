package com.mowitnow.tondeuse.tondeuseautomatique.config;

import com.mowitnow.tondeuse.tondeuseautomatique.model.TondeuseInput;
import com.mowitnow.tondeuse.tondeuseautomatique.model.TondeuseOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

@Configuration
@EnableBatchProcessing
public class SpringBatchConfig {

    private final Logger logger = LoggerFactory.getLogger(SpringBatchConfig.class);

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Value("${input.file.path}")
    private Resource fichierEntree;

    @Value("${output.file.path}")
    private String cheminFichierSortie;

    private int largeur;
    private int hauteur;

    @Bean
    public FlatFileItemReader<TondeuseInput> reader() {
        return new FlatFileItemReaderBuilder<TondeuseInput>()
                .name("tondeuseInputReader")
                .resource(fichierEntree)
                .linesToSkip(1)
                .delimited()
                .delimiter(" ")
                .names("x", "y", "orientation", "commande")
                .targetType(TondeuseInput.class)
                .build();
    }

    @Bean
    public ItemProcessor<TondeuseInput, TondeuseOutput> tondeuseProcessor() {
        return tondeuseInput -> {
            //Récuperation des dimensions de la Pelouse
            if (largeur == 0 || hauteur == 0) {
                int[] pelouse = lireDimPelouse();
                largeur = pelouse[0];
                hauteur = pelouse[1];
                logger.debug("Dimensions de la Pelouse : hauteur :" + hauteur + " et largeur :" + largeur);
            }
            TondeuseOutput tondeuseOutput = new TondeuseOutput();
            tondeuseOutput.setXm(tondeuseInput.getX0());
            tondeuseOutput.setYm(tondeuseInput.getY0());
            tondeuseOutput.setOrientationm(tondeuseInput.getOrientation0());
            for (char command : tondeuseInput.getCommande().toCharArray()) {
                processCommand(tondeuseOutput, command, largeur, hauteur);
            }
            return tondeuseOutput;
        };
    }

    @Bean
    public ItemWriter<TondeuseOutput> writer() {
        return new FlatFileItemWriterBuilder<TondeuseOutput>()
                .name("itemWriter")
                .resource(new FileSystemResource(cheminFichierSortie))
                .lineAggregator(item -> {
                    String result = item.getXm() + " " + item.getYm() + " " + item.getOrientationm();
                    logger.debug("Ligne fichier sortie: " + result);
                    return result;
                })
                .build();
    }

    @Bean
    public Step processTondeuseStep() throws IOException {
        return stepBuilderFactory.get("processTondeuseStep")
                .<TondeuseInput, TondeuseOutput>chunk(1)
                .reader(reader())
                .processor(tondeuseProcessor())
                .writer(writer())
                .build();
    }

    @Bean
    public Job tondeuseJob(Step processTondeuseStep) {
        return jobBuilderFactory.get("tondeuseJob")
                .flow(processTondeuseStep)
                .end()
                .build();
    }

    private int[] lireDimPelouse() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(fichierEntree.getFile()));
            String premiereLigne = reader.readLine();
            String[] pelouse = premiereLigne.split(" ");
            int l = Integer.parseInt(pelouse[0]);
            int h = Integer.parseInt(pelouse[1]);
            reader.close();
            return new int[]{l, h};
        } catch (IOException e) {
            e.printStackTrace();
            return new int[]{0, 0};
        }
    }


    private void processCommand(TondeuseOutput tondeuseOutput, char command, int l, int h) {
        switch (command) {
            case 'G':
                turnLeft(tondeuseOutput);
                break;
            case 'D':
                turnRight(tondeuseOutput);
                break;
            case 'A':
                moveForward(tondeuseOutput, l, h);
                break;
            default:
                break;
        }
    }

    private void turnLeft(TondeuseOutput tondeuseOutput) {
        char orientation_n = tondeuseOutput.getOrientationm();
        switch (orientation_n) {
            case 'N':
                tondeuseOutput.setOrientationm('W');
                break;
            case 'W':
                tondeuseOutput.setOrientationm('S');
                break;
            case 'S':
                tondeuseOutput.setOrientationm('E');
                break;
            case 'E':
                tondeuseOutput.setOrientationm('N');
                break;
        }
    }

    private void turnRight(TondeuseOutput tondeuseOutput) {
        char orientation_n = tondeuseOutput.getOrientationm();
        switch (orientation_n) {
            case 'N':
                tondeuseOutput.setOrientationm('E');
                break;
            case 'E':
                tondeuseOutput.setOrientationm('S');
                break;
            case 'S':
                tondeuseOutput.setOrientationm('W');
                break;
            case 'W':
                tondeuseOutput.setOrientationm('N');
                break;
        }
    }

    private void moveForward(TondeuseOutput tondeuseOutput, int l, int h) {
        char orientation_n = tondeuseOutput.getOrientationm();
        int xn = tondeuseOutput.getXm();
        int yn = tondeuseOutput.getYm();

        switch (orientation_n) {
            case 'N':
                tondeuseOutput.setYm(yn + 1);
                break;
            case 'E':
                tondeuseOutput.setXm(xn + 1);
                break;
            case 'S':
                tondeuseOutput.setYm(yn - 1);
                break;
            case 'W':
                tondeuseOutput.setXm(xn - 1);
                break;
        }

        if (isValidPosition(tondeuseOutput.getXm(), tondeuseOutput.getYm(), l, h)) {
            tondeuseOutput.setXm(tondeuseOutput.getXm());
            tondeuseOutput.setYm(tondeuseOutput.getYm());
        }
    }

    private boolean isValidPosition(int x, int y, int l, int h) {
        return x >= 0 && x <= l && y >= 0 && y <= h;
    }

}

