package ru.sber.kvitmoscow.handler.file;

import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import ru.sber.kvitmoscow.Authorization;
import ru.sber.kvitmoscow.handler.file.data.ExcelFileData;
import ru.sber.kvitmoscow.handler.file.data.FileData;
import ru.sber.kvitmoscow.handler.file.data.TextFileData;
import ru.sber.kvitmoscow.handler.file.model.*;
import ru.sber.kvitmoscow.handler.pdf.PdfHandler;
import ru.sber.kvitmoscow.model.UserSetting;
import ru.sber.kvitmoscow.service.*;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class FileHandler {
    @Autowired
    private UserSettingService userSettingService;

    @Autowired
    private FileMainFieldService fileMainFieldService;

    @Autowired
    private FileUniqueFieldService fileUniqueFieldService;

    @Autowired
    private FileSumFieldService fileSumFieldService;

    @Autowired
    private FileCounterFieldService fileCounterFieldService;

    public ByteArrayOutputStream handle(MultipartFile file, int userSettingId) throws Exception {
        //get user columns from settings
        List<String> columnNameListFromSettings = new ArrayList<>();

        MainColEntity mainColumns = new MainColEntity();
        List<UniqueColEntity> uniqueColumnList = new ArrayList<>();
        List<SumColEntity> sumColumnList = new ArrayList<>();
        List<CounterColEntity> counterColumnList = new ArrayList<>();

        fileMainFieldService.getAllByUserSettingId(userSettingId).forEach(f -> {
            columnNameListFromSettings.add(f.getLs());
            columnNameListFromSettings.add(f.getFio());
            columnNameListFromSettings.add(f.getAdr());
            columnNameListFromSettings.add(f.getPeriod());

            mainColumns.setLs(f.getLs());
            mainColumns.setAdr(f.getAdr());
            mainColumns.setFio(f.getFio());
            mainColumns.setPeriod(f.getPeriod());

            mainColumns.setLsName(f.getLsName());
            mainColumns.setAdrName(f.getAdrName());
            mainColumns.setFioName(f.getFioName());
            mainColumns.setPeriodName(f.getPeriodName());
        });

        fileUniqueFieldService.getAllByUserSettingId(userSettingId).forEach(f -> {
            columnNameListFromSettings.add(f.getValue());

            uniqueColumnList.add(new UniqueColEntity(f.getName(), f.getValue()));
        });

        fileSumFieldService.getAllByUserSettingId(userSettingId).forEach(f -> {
            columnNameListFromSettings.add(f.getName());
            columnNameListFromSettings.add(f.getCurrent());
            columnNameListFromSettings.add(f.getDebt());
            columnNameListFromSettings.add(f.getTotal());

            sumColumnList.add(new SumColEntity(f.getName(), f.getCurrent(), f.getDebt(), f.getTotal()));
        });

        fileCounterFieldService.getAllByUserSettingId(userSettingId).forEach(f -> {
            columnNameListFromSettings.add(f.getName());
            columnNameListFromSettings.add(f.getMesure());
            columnNameListFromSettings.add(f.getConsumption());
            columnNameListFromSettings.add(f.getCurrent());
            columnNameListFromSettings.add(f.getPrevious());
            columnNameListFromSettings.add(f.getTariff());

            counterColumnList.add(new CounterColEntity(f.getName(), f.getMesure(), f.getConsumption(), f.getCurrent(), f.getPrevious(), f.getTariff()));
        });

        List<String> columnNameListFromSettingsWithNoEmpty = columnNameListFromSettings.stream().filter(e -> !e.isEmpty()).collect(Collectors.toList());

        InputStream inputStream = file.getInputStream();

        String fileName = Objects.requireNonNull(file.getOriginalFilename()).toUpperCase();
        String extension = FilenameUtils.getExtension(fileName);


        FileData fileData = null;
        if (extension.equals("XLS") || extension.equals("XLSX")){
            fileData = new ExcelFileData(inputStream, extension, columnNameListFromSettingsWithNoEmpty);
        } else if (extension.equals("TXT") || extension.equals("CSV")) {
            fileData = new TextFileData(inputStream, columnNameListFromSettingsWithNoEmpty);
        }

        //get pay reqs ???????
        List<UserSetting> userSettings = userSettingService.getAllByUserId(Authorization.id());
        UserSetting payReqs = userSettings.get(0);

        List<FileRow> fileRowList = fileData.handle();
        List<String> columnNameListFromFile = fileData.getColumnNameListFromFile();

        ByteArrayOutputStream baos = new PdfHandler().handle(fileRowList, payReqs, mainColumns, sumColumnList, uniqueColumnList,  counterColumnList, columnNameListFromFile);
        return baos;
    }
}
