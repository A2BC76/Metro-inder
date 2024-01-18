package com.example.metroinder.dataSet.service;


import com.example.metroinder.dataSet.dto.CapitalareaStationDto;
import com.example.metroinder.dataSet.dto.LineDto;
import com.example.metroinder.dataSet.dto.StationLineDto;
import com.example.metroinder.dataSet.dto.TimeStationPersonnelDto;
import com.example.metroinder.dataSet.model.*;
import com.example.metroinder.dataSet.repository.*;
import com.example.metroinder.stationSchedule.dto.StationScheduleDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
@Component
public class StationInformationSetService {

    @Value("${generalKey}")
    String generalKey;
    Pattern PATTERN_BRACKET = Pattern.compile("\\([^\\(\\)]+\\)");
    String VOID = "";

    private final CapitalareaStationRepository capitalareaStationRepository;
    private final LineRepository lineRepository;
    private final StationLineRepository stationLineRepository;
    private final TimeStationPersonnelRepository timeStationPersonnelRepository;
    private final StationScheduleRepository stationScheduleRepository;

    // 서울시 지하철 호선별 역별 시간대별 승하차 인원 정보 API 호출 및 저장
    public void peopleInformationBySeoulAtTimeSave(String dataScope)  throws IOException {
        try {
            StringBuilder urlBuilder = new StringBuilder("http://openapi.seoul.go.kr:8088");
            urlBuilder.append("/" + URLEncoder.encode(generalKey, "UTF-8"));
            urlBuilder.append("/" + URLEncoder.encode("json", "UTF-8"));
            urlBuilder.append("/" + URLEncoder.encode("CardSubwayTime", "UTF-8"));
            urlBuilder.append("/" + URLEncoder.encode("1", "UTF-8"));
            urlBuilder.append("/" + URLEncoder.encode("999", "UTF-8"));

            /* 서비스별 추가 요청인자*/
            urlBuilder.append("/" + URLEncoder.encode(dataScope, "UTF-8"));//월별, 현재 최신 2022년 10월까지

            URL url = new URL(urlBuilder.toString());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Content-type", "application/json");
            //System.out.println("Response code: " + conn.getResponseCode()); /* 연결에 대한 확인*/
            BufferedReader rd;

            // 서비스코드가 정상이면 200~300사이의 숫자
            if (conn.getResponseCode() >= 200 && conn.getResponseCode() <= 300) {
                rd = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            } else {
                rd = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "UTF-8"));
            }
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = rd.readLine()) != null) {
                stringBuilder.append(line);
            }
            JSONParser jsonParser = new JSONParser();
            JSONObject jsonObject = (JSONObject) jsonParser.parse(stringBuilder.toString());
            JSONObject cardSubwayTime = (JSONObject) jsonObject.get("CardSubwayTime");
            /* 1000건이 넘는 월 데이터가 있었으나 데이터가 수정됬는지 없어짐. 불필요한 코드 */
            /*Long total = ((Long) cardSubwayTime.get("list_total_count"));
            if(total.intValue() > 999) {
                urlBuilder = new StringBuilder("http://openapi.seoul.go.kr:8088");
                urlBuilder.append("/" + URLEncoder.encode(generalKey, "UTF-8"));
                urlBuilder.append("/" + URLEncoder.encode("json", "UTF-8"));
                urlBuilder.append("/" + URLEncoder.encode("CardSubwayTime", "UTF-8"));
                urlBuilder.append("/" + URLEncoder.encode("1000", "UTF-8"));
                urlBuilder.append("/" + URLEncoder.encode("1999", "UTF-8"));
                urlBuilder.append("/" + URLEncoder.encode(dataScope, "UTF-8"));
                url = new URL(urlBuilder.toString());
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Content-type", "application/json");
                //System.out.println("Response code: " + conn.getResponseCode());

                if (conn.getResponseCode() >= 200 && conn.getResponseCode() <= 300) {
                    rd = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                } else {
                    rd = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "UTF-8"));
                }
                while ((line = rd.readLine()) != null) {
                    stringBuilder.append(line);
                }
                jsonObject = (JSONObject) jsonParser.parse(stringBuilder.toString());
                cardSubwayTime = (JSONObject) jsonObject.get("CardSubwayTime");
            }*/
            rd.close();
            conn.disconnect();
            JSONArray jsonArr = (JSONArray) cardSubwayTime.get("row");
            List<TimeStationPersonnelDto> jsonSameStationDtoList = new ArrayList<>();
            for (int count = 0; count < jsonArr.size(); count++) {
                JSONObject row = (JSONObject) jsonArr.get(count);
                String recordMonth = (String) row.get("USE_MON");
                String station = deleteBracket((String) row.get("SUB_STA_NM"));
                String stnLine = (String) row.get("LINE_NUM");
                switch(stnLine) {
                    case "경원선":
                        if(station.equals("소요산") || station.equals("동두천") || station.equals("보산") || station.equals("동두천중앙") || station.equals("지행") || station.equals("덕정") || station.equals("덕계") || station.equals("양주") || station.equals("녹양") || station.equals("가능") || station.equals("의정부") ||station.equals("회룡") || station.equals("망월사") || station.equals("도봉산") || station.equals("도봉")  || station.equals("방학") || station.equals("창동") || station.equals("녹천") || station.equals("월계") || station.equals("광운대") || station.equals("석계") || station.equals("신이문") || station.equals("외대앞")) {
                            stnLine = "경인1호선";
                        } else if (station.equals("청량리") || station.equals("왕십리")) {
                            stnLine = "수인분당선";
                        }
                        break;
                    case "중앙선":
                        if(station.equals("회기")) {
                            stnLine = "경인1호선";
                        }
                        break;
                    case "경부선":
                        if (station.equals("남영") || station.equals("용산") || station.equals("노량진") || station.equals("대방") || station.equals("신길") || station.equals("영등포") || station.equals("신도림")) {
                            stnLine = "경인1호선";
                        } else if(station.equals("구로") || station.equals("가산디지털단지") || station.equals("독산") || station.equals("석수") || station.equals("관악") || station.equals("안양") || station.equals("명학") || station.equals("군포") || station.equals("당정") ||station.equals("의왕") || station.equals("성균관대") || station.equals("화서") || station.equals("수원")  || station.equals("세류") || station.equals("세마") || station.equals("오산대") || station.equals("오산") || station.equals("진위") || station.equals("송탄") || station.equals("서정리") || station.equals("평택지제") || station.equals("평택") || station.equals("성환") || station.equals("직산") || station.equals("두정") || station.equals("천안") || station.equals("병점") || station.equals("금천구청") || station.equals("금정")) {
                            stnLine = "경부1호선";
                        } else if (station.equals("서동탄")) {
                            stnLine = "병점기지선";
                        }
                        break;
                    case "경인선":
                        if(station.equals("구로") || station.equals("구일") || station.equals("개봉") || station.equals("오류동") || station.equals("온수") || station.equals("역곡") || station.equals("소사") || station.equals("부천") || station.equals("중동") || station.equals("송내") || station.equals("부개") || station.equals("부평") || station.equals("백운") || station.equals("동암") || station.equals("간석") || station.equals("주안") || station.equals("도화") || station.equals("제물포") || station.equals("도원") || station.equals("동인천") || station.equals("인천")) {
                            stnLine = "경인1호선";
                        }
                        break;
                    case "장항선":
                        if(station.equals("봉명") || station.equals("쌍용") || station.equals("아산") || station.equals("탕정") || station.equals("배방") || station.equals("온양온천") || station.equals("신창")) {
                            stnLine = "경부1호선";
                        }
                        break;
                    case "1호선":
                        if(station.equals("청량리") || station.equals("제기동") || station.equals("신설동") || station.equals("동묘앞") || station.equals("동대문") || station.equals("종로5가") || station.equals("종로3가") || station.equals("종각") || station.equals("시청") || station.equals("서울역")) {
                            stnLine = "경인1호선";
                        }
                        break;
                    case "2호선":
                        stnLine = "서울2호선";
                        if(station.equals("용답") || station.equals("신답") || station.equals("용두") || station.equals("신설동")) {
                            stnLine ="성수지선";
                        }
                        if(station.equals("도림천") || station.equals("양천구청") || station.equals("신정네거리")) {
                            stnLine ="신정지선";
                        }
                        break;
                    case "3호선":
                        stnLine = "수도권3호선";
                        break;
                    case "4호선":
                        stnLine = "수도권4호선";
                        break;
                    case "5호선":
                        if(station.equals("방화") || station.equals("개화산") || station.equals("김포공항") || station.equals("송정") || station.equals("마곡") || station.equals("발산") || station.equals("우장산") || station.equals("화곡") || station.equals("까치산") || station.equals("신정") || station.equals("목동") || station.equals("오목교") || station.equals("양평") || station.equals("영등포구청") || station.equals("영등포시장") || station.equals("신길") || station.equals("여의도") || station.equals("여의나루") || station.equals("마포") || station.equals("공덕") || station.equals("애오개") || station.equals("충정로") || station.equals("서대문") || station.equals("광화문") || station.equals("종로3가") || station.equals("을지로4가") || station.equals("동대문역사문화공원") || station.equals("청구") || station.equals("신금호") || station.equals("행당") || station.equals("왕십리") || station.equals("마장") || station.equals("답십리") || station.equals("장한평") || station.equals("군자") || station.equals("아차산") || station.equals("광나루") || station.equals("천호") || station.equals("강동") || station.equals("길동") || station.equals("굽은다리") || station.equals("명일") || station.equals("고덕") || station.equals("상일동")) {
                            stnLine = "수도권5호선";
                        } else if(station.equals("둔촌동") || station.equals("올림픽공원") || station.equals("방이") || station.equals("오금") || station.equals("개롱") || station.equals("거여") || station.equals("마천")) {
                            stnLine = "마천지선";
                        }
                        break;
                    case "6호선":
                        stnLine = "서울6호선";
                        break;
                    case "7호선":
                        stnLine = "서울7호선";
                        break;
                    case "8호선":
                        stnLine = "서울8호선";
                        break;
                    case "9호선":
                        stnLine = "서울9호선";
                        break;
                    case "9호선2~3단계":
                        stnLine = "서울9호선";
                        break;
                    case "과천선":
                        if(station.equals("선바위") || station.equals("경마공원") || station.equals("대공원") || station.equals("과천") || station.equals("정부과천청사") || station.equals("인덕원") || station.equals("평촌") || station.equals("범계")) {
                            stnLine = "수도권4호선";
                        }
                        break;
                    case "안산선":
                        if(station.equals("산본") || station.equals("수리산") || station.equals("대야미") || station.equals("반월") || station.equals("상록수") || station.equals("한대앞") || station.equals("중앙") || station.equals("고잔") || station.equals("초지") || station.equals("안산") || station.equals("신길온천") || station.equals("정왕") || station.equals("오이도")) {
                            stnLine = "수도권4호선";
                        }
                        if(station.equals("한대앞") || station.equals("중앙") || station.equals("고잔") || station.equals("초지") || station.equals("안산") || station.equals("신길온천") || station.equals("정왕") || station.equals("오이도")) {
                            stnLine = "수인분당선";
                        }
                        break;
                    case "분당선":
                        stnLine = "수인분당선";
                        break;
                    case "수인선":
                        stnLine = "수인분당선";
                        break;
                    case "공항철도 1호선":
                        stnLine = "공항철도";
                        break;
                    case "일산선":
                        if(station.equals("대화") || station.equals("주엽") || station.equals("정발산") || station.equals("마두") || station.equals("백석") || station.equals("대곡") || station.equals("화정") || station.equals("원당") || station.equals("원흥") || station.equals("삼송")) {
                            stnLine = "수도권3호선";
                        }
                        break;
                    default:
                        break;
                }

                if(station.equals("서울역")) {
                    station = "서울";
                }
                TimeStationPersonnelDto timeStationPersonnelDto = TimeStationPersonnelDto.builder()
                        .station(station)
                        .line(stnLine)
                        .oneRide((int) Math.round((double) row.get("ONE_RIDE_NUM")))
                        .twoRide((int) Math.round((double) row.get("TWO_RIDE_NUM")))
                        .threeRide((int) Math.round((double) row.get("THREE_RIDE_NUM")))
                        .fourRide((int) Math.round((double) row.get("FOUR_RIDE_NUM")))
                        .fiveRide((int) Math.round((double) row.get("FIVE_RIDE_NUM")))
                        .sixRide((int) Math.round((double) row.get("SIX_RIDE_NUM")))
                        .sevenRide((int) Math.round((double) row.get("SEVEN_RIDE_NUM")))
                        .eightRide((int) Math.round((double) row.get("EIGHT_RIDE_NUM")))
                        .nineRide((int) Math.round((double) row.get("NINE_RIDE_NUM")))
                        .tenRide((int) Math.round((double) row.get("TEN_RIDE_NUM")))
                        .elevenRide((int) Math.round((double) row.get("ELEVEN_RIDE_NUM")))
                        .twelveRide((int) Math.round((double) row.get("TWELVE_RIDE_NUM")))
                        .thirteenRide((int) Math.round((double) row.get("THIRTEEN_RIDE_NUM")))
                        .fourteenRide((int) Math.round((double) row.get("FOURTEEN_RIDE_NUM")))
                        .fifteenRide((int) Math.round((double) row.get("FIFTEEN_RIDE_NUM")))
                        .sixteenRide((int) Math.round((double) row.get("SIXTEEN_RIDE_NUM")))
                        .seventeenRide((int) Math.round((double) row.get("SEVENTEEN_RIDE_NUM")))
                        .eighteenRide((int) Math.round((double) row.get("EIGHTEEN_RIDE_NUM")))
                        .nineteenRide((int) Math.round((double) row.get("NINETEEN_RIDE_NUM")))
                        .twentyRide((int) Math.round((double) row.get("TWENTY_RIDE_NUM")))
                        .twentyoneRide((int) Math.round((double) row.get("TWENTY_ONE_RIDE_NUM")))
                        .twentytwoRide((int) Math.round((double) row.get("TWENTY_TWO_RIDE_NUM")))
                        .twentythreeRide((int) Math.round((double) row.get("TWENTY_THREE_RIDE_NUM")))
                        .midnightRide((int) Math.round((double) row.get("MIDNIGHT_RIDE_NUM")))
                        .recordMonth(recordMonth)
                        .build();
                jsonSameStationDtoList.add(timeStationPersonnelDto);
                if(station.equals("구로") || station.equals("금정") || station.equals("병점") || station.equals("성수") || station.equals("신도림") || station.equals("까치산") || station.equals("총신대입구") || station.equals("한대앞") || station.equals("중앙") || station.equals("고잔") || station.equals("초지") || station.equals("안산") || station.equals("신길온천") || station.equals("정왕") || station.equals("오이도") || station.equals("강동")) {
                    if(station.equals("구로") && stnLine.equals("경부1호선")) {
                        stnLine = "경인1호선";
                    }else if(station.equals("금정") && stnLine.equals("경부1호선")) {
                        stnLine = "수도권4호선";
                    } else if(station.equals("병점") && stnLine.equals("경부1호선")) {
                        stnLine = "병점기지선";
                    } else if(station.equals("성수") && stnLine.equals("서울2호선")) {
                        stnLine = "성수지선";
                    } else if(station.equals("신도림") && stnLine.equals("서울2호선")) {
                        stnLine = "신정지선";
                    } else if(station.equals("까치산") && stnLine.equals("수도권5호선")) {
                        stnLine = "신정지선";
                    } else if(station.equals("총신대입구") && stnLine.equals("서울7호선")) {
                        stnLine = "수도권4호선";
                    } else if((station.equals("한대앞")&& stnLine.equals("수인분당선")) || (station.equals("중앙")&& stnLine.equals("수인분당선")) || (station.equals("고잔")&& stnLine.equals("수인분당선")) || (station.equals("초지")&& stnLine.equals("수인분당선")) || (station.equals("안산")&& stnLine.equals("수인분당선")) || (station.equals("신길온천")&& stnLine.equals("수인분당선")) || (station.equals("정왕")&& stnLine.equals("수인분당선")) || (station.equals("오이도") && stnLine.equals("수인분당선")) || (station.equals("한대앞") && stnLine.equals("수인분당선"))) {
                        stnLine = "수도권4호선";
                    } else if(station.equals("강동")) {
                        stnLine = "마천지선";
                    }
                    timeStationPersonnelDto = TimeStationPersonnelDto.builder()
                            .station(station)
                            .line(stnLine)
                            .oneRide((int) Math.round((double) row.get("ONE_RIDE_NUM")))
                            .twoRide((int) Math.round((double) row.get("TWO_RIDE_NUM")))
                            .threeRide((int) Math.round((double) row.get("THREE_RIDE_NUM")))
                            .fourRide((int) Math.round((double) row.get("FOUR_RIDE_NUM")))
                            .fiveRide((int) Math.round((double) row.get("FIVE_RIDE_NUM")))
                            .sixRide((int) Math.round((double) row.get("SIX_RIDE_NUM")))
                            .sevenRide((int) Math.round((double) row.get("SEVEN_RIDE_NUM")))
                            .eightRide((int) Math.round((double) row.get("EIGHT_RIDE_NUM")))
                            .nineRide((int) Math.round((double) row.get("NINE_RIDE_NUM")))
                            .tenRide((int) Math.round((double) row.get("TEN_RIDE_NUM")))
                            .elevenRide((int) Math.round((double) row.get("ELEVEN_RIDE_NUM")))
                            .twelveRide((int) Math.round((double) row.get("TWELVE_RIDE_NUM")))
                            .thirteenRide((int) Math.round((double) row.get("THIRTEEN_RIDE_NUM")))
                            .fourteenRide((int) Math.round((double) row.get("FOURTEEN_RIDE_NUM")))
                            .fifteenRide((int) Math.round((double) row.get("FIFTEEN_RIDE_NUM")))
                            .sixteenRide((int) Math.round((double) row.get("SIXTEEN_RIDE_NUM")))
                            .seventeenRide((int) Math.round((double) row.get("SEVENTEEN_RIDE_NUM")))
                            .eighteenRide((int) Math.round((double) row.get("EIGHTEEN_RIDE_NUM")))
                            .nineteenRide((int) Math.round((double) row.get("NINETEEN_RIDE_NUM")))
                            .twentyRide((int) Math.round((double) row.get("TWENTY_RIDE_NUM")))
                            .twentyoneRide((int) Math.round((double) row.get("TWENTY_ONE_RIDE_NUM")))
                            .twentytwoRide((int) Math.round((double) row.get("TWENTY_TWO_RIDE_NUM")))
                            .twentythreeRide((int) Math.round((double) row.get("TWENTY_THREE_RIDE_NUM")))
                            .midnightRide((int) Math.round((double) row.get("MIDNIGHT_RIDE_NUM")))
                            .recordMonth(recordMonth)
                            .build();
                    jsonSameStationDtoList.add(timeStationPersonnelDto);
                }
            }
            TimeStationPersonnelDto timeStationPersonnelDto = new TimeStationPersonnelDto();
            List<TimeStationPersonnel> jsonSameStationList = timeStationPersonnelDto.toEntityList(jsonSameStationDtoList);
            for (TimeStationPersonnel timeStationPersonnel : jsonSameStationList) {
                timeStationPersonnelRepository.save(timeStationPersonnel);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setStationInformation()  throws IOException {
        try {
            JSONParser jsonParser = new JSONParser();
            ClassPathResource resource = new ClassPathResource("static/json/lines.json");
            JSONArray jsonArray = (JSONArray) jsonParser.parse(new InputStreamReader(resource.getInputStream(), "UTF-8"));
            String stnLine = "";
            int routeOrder = 1;

            for (int count = 0; count < jsonArray.size(); count++) {
                JSONObject row = (JSONObject) jsonArray.get(count);
                String jsonStnLine = (String) row.get("stn_line");
                String jsonStnName = (String) row.get("stn_name");
                String jsonStnTransfer = (String) row.get("stn_transfer");

                if(jsonStnLine.equals(stnLine)) {
                    stnLine = jsonStnLine;
                    routeOrder++;
                } else {
                    stnLine = jsonStnLine;
                    routeOrder = 1;
                }

                CapitalareaStation capitalareaStation = new CapitalareaStation();
                Line line = new Line();

                if(overlapCheck(jsonStnName, "station")) {
                    CapitalareaStationDto capitalareaStationDto = new CapitalareaStationDto();
                    capitalareaStation = capitalareaStationDto.toEntity(CapitalareaStationDto.builder()
                            .station(jsonStnName).build());
                } else {
                    capitalareaStation = capitalareaStationRepository.findByStation(jsonStnName);
                }
                if(overlapCheck(jsonStnLine, "line")) {
                    LineDto lineDto = new LineDto();
                    line = lineDto.toEntity(LineDto.builder()
                            .line(jsonStnLine).build());
                } else {
                    line = lineRepository.findByLine(jsonStnLine);
                }

                StationLineDto stationLineDto = new StationLineDto();
                StationLine stationLine = stationLineDto.toEntity(StationLineDto.builder()
                        .capitalareaStation(capitalareaStation)
                        .line(line)
                        .lineOrder(routeOrder).build());
                capitalareaStationRepository.save(capitalareaStation);
                lineRepository.save(line);
                stationLineRepository.save(stationLine);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean overlapCheck(String checkValue, String division) {
        if(division.equals("station")) {
            if (capitalareaStationRepository.findByStation(checkValue) != null) {
                return false;
            }
            return true;
        }else {
            if (lineRepository.findByLine(checkValue) != null) {
                return false;
            }
            return true;
        }
    }

    public void setLetLon() throws IOException, ParseException, NullPointerException {
        try {
            JSONParser jsonParser = new JSONParser();
            ClassPathResource resource = new ClassPathResource("static/json/station_coordinate.json");
            JSONArray jsonArray = (JSONArray) jsonParser.parse(new InputStreamReader(resource.getInputStream(), "UTF-8"));
            //log.info(""+ jsonArray);
            for (int count = 0; count < jsonArray.size(); count++) {
                JSONObject row = (JSONObject) jsonArray.get(count);
                String station = (String) row.get("name");
                if(station.equals("서울역")) {
                    station = "서울";
                }
                Double lat = (Double) row.get("lat");
                Double lng = (Double) row.get("lng");
                // log.info(""+ station + " : " + "위도 : " + lat + ", 경도 : " + lng);
                CapitalareaStation capitalareaStation = capitalareaStationRepository.findByStation(station);
                if(capitalareaStation == null || capitalareaStation.getLat() != null && capitalareaStation.getLng() != null) {
                    continue;
                }
                capitalareaStation.setLat(lat);
                capitalareaStation.setLng(lng);
                capitalareaStationRepository.save(capitalareaStation);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //혼잡도 평균 List 생성
    public void getStationDegreeOfCongestionAvg(int count) {
        /*List<StationLine> stationLineList = new ArrayList<>();*/
        List<StationLine> stationLines = stationLineRepository.findAll();
        for(StationLine stationLine : stationLines) {
            String stationName = stationLine.getCapitalareaStation().getStation();
            String lineName = stationLine.getLine().getLine();

            TimeStationPersonnelRepository.SameStationPeople sameStationPeople = timeStationPersonnelRepository.stationDegreeOfCongestion(count, stationName, lineName);
            if(sameStationPeople == null) {
                continue;
            }
            stationLine.setOneRide(Long.valueOf(sameStationPeople.getOneRide()).intValue());
            stationLine.setTwoRide(Long.valueOf(sameStationPeople.getTwoRide()).intValue());
            stationLine.setThreeRide(Long.valueOf(sameStationPeople.getThreeRide()).intValue());
            stationLine.setFourRide(Long.valueOf(sameStationPeople.getFourRide()).intValue());
            stationLine.setFiveRide(Long.valueOf(sameStationPeople.getFiveRide()).intValue());
            stationLine.setSixRide(Long.valueOf(sameStationPeople.getSixRide()).intValue());
            stationLine.setSevenRide(Long.valueOf(sameStationPeople.getSevenRide()).intValue());
            stationLine.setEightRide(Long.valueOf(sameStationPeople.getEightRide()).intValue());
            stationLine.setNineRide(Long.valueOf(sameStationPeople.getNineRide()).intValue());
            stationLine.setTenRide(Long.valueOf(sameStationPeople.getTenRide()).intValue());
            stationLine.setElevenRide(Long.valueOf(sameStationPeople.getElevenRide()).intValue());
            stationLine.setTwelveRide(Long.valueOf(sameStationPeople.getTwelveRide()).intValue());
            stationLine.setThirteenRide(Long.valueOf(sameStationPeople.getThirteenRide()).intValue());
            stationLine.setFourteenRide(Long.valueOf(sameStationPeople.getFourteenRide()).intValue());
            stationLine.setFifteenRide(Long.valueOf(sameStationPeople.getFifteenRide()).intValue());
            stationLine.setSixteenRide(Long.valueOf(sameStationPeople.getSixteenRide()).intValue());
            stationLine.setSeventeenRide(Long.valueOf(sameStationPeople.getSeventeenRide()).intValue());
            stationLine.setEighteenRide(Long.valueOf(sameStationPeople.getEighteenRide()).intValue());
            stationLine.setNineteenRide(Long.valueOf(sameStationPeople.getNineteenRide()).intValue());
            stationLine.setTwentyRide(Long.valueOf(sameStationPeople.getTwentyRide()).intValue());
            stationLine.setTwentyoneRide(Long.valueOf(sameStationPeople.getTwentyoneRide()).intValue());
            stationLine.setTwentytwoRide(Long.valueOf(sameStationPeople.getTwentytwoRide()).intValue());
            stationLine.setTwentythreeRide(Long.valueOf(sameStationPeople.getTwentythreeRide()).intValue());
            stationLine.setMidnightRide(Long.valueOf(sameStationPeople.getMidnightRide()).intValue());

            stationLineRepository.save(stationLine);
        }
    }

    /* 입력한 역이름으로 역코드목록을 json으로 받아옴 */
    public String getStationCode(String station) throws IOException {
        StringBuilder urlBuilder = new StringBuilder("http://openAPI.seoul.go.kr:8088");
        urlBuilder.append("/" + URLEncoder.encode(generalKey, "UTF-8"));
        urlBuilder.append("/" + URLEncoder.encode("json", "UTF-8"));
        urlBuilder.append("/" + URLEncoder.encode("SearchInfoBySubwayNameService", "UTF-8"));
        urlBuilder.append("/" + URLEncoder.encode("1", "UTF-8"));
        urlBuilder.append("/" + URLEncoder.encode("999", "UTF-8"));

        /* 서비스별 추가 요청인자*/
        /* 전철역이름 */
        if(station.equals("서울")) {
            station = "서울역";
        }
        urlBuilder.append("/" + URLEncoder.encode(station, "UTF-8"));

        URL url = new URL(urlBuilder.toString());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Content-type", "application/json");
        //System.out.println("Response code: " + conn.getResponseCode()); /* 연결에 대한 확인*/
        BufferedReader rd;

        // 서비스코드가 정상이면 200~300사이의 숫자
        if (conn.getResponseCode() >= 200 && conn.getResponseCode() <= 300) {
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
        } else {
            rd = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "UTF-8"));
        }
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = rd.readLine()) != null) {
            stringBuilder.append(line);
        }
        rd.close();
        conn.disconnect();
        return stringBuilder.toString();
    }

    public String getStationSchduleAPI(String stationCode, String week, String inout) throws IOException {
        StringBuilder urlBuilder = new StringBuilder("http://openAPI.seoul.go.kr:8088");
        urlBuilder.append("/" + URLEncoder.encode(generalKey, "UTF-8"));
        urlBuilder.append("/" + URLEncoder.encode("json", "UTF-8"));
        urlBuilder.append("/" + URLEncoder.encode("SearchSTNTimeTableByIDService", "UTF-8"));
        urlBuilder.append("/" + URLEncoder.encode("1", "UTF-8"));
        urlBuilder.append("/" + URLEncoder.encode("999", "UTF-8"));

        /* 서비스별 추가 요청인자*/
        /* 전철역코드 */
        urlBuilder.append("/" + URLEncoder.encode(stationCode, "UTF-8"));
        /* 요일 */
        urlBuilder.append("/" + URLEncoder.encode(week, "UTF-8"));
        /* 상/하행선 */
        urlBuilder.append("/" + URLEncoder.encode(inout, "UTF-8"));

        URL url = new URL(urlBuilder.toString());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Content-type", "application/json");
        //System.out.println("Response code: " + conn.getResponseCode());
        BufferedReader rd;

        // 서비스코드가 정상이면 200~300사이의 숫자
        if (conn.getResponseCode() >= 200 && conn.getResponseCode() <= 300) {
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
        } else {
            rd = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "UTF-8"));
        }
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = rd.readLine()) != null) {
            stringBuilder.append(line);
        }
        rd.close();
        conn.disconnect();
        return stringBuilder.toString();
    }

    /* JSON List에서 역코드를 뽑아와 열차 시간표 API 호출 */
    public void stationScheduleCall(String json, String week, String inout) {
        try {
            JSONParser jsonParser = new JSONParser();
            JSONObject jsonObject = (JSONObject) jsonParser.parse(json);
            JSONObject searchInfoBySubwayName = (JSONObject) jsonObject.get("SearchInfoBySubwayNameService");
            //log.info("json("+ i + ") : " + searchInfoBySubwayName);
            JSONArray jsonArr = (JSONArray) searchInfoBySubwayName.get("row");
            //JSONArray result = (JSONArray) searchInfoBySubwayName.get("RESULT");
            List<String> stationScheduleList = new ArrayList<>();
            for (int count = 0; count < jsonArr.size(); count++) {
                JSONObject row = (JSONObject) jsonArr.get(count);
                String stationCode = (String) row.get("STATION_CD");

                String lineNum = (String) row.get("LINE_NUM");
                if(lineNum.equals("01호선") || lineNum.equals("02호선") || lineNum.equals("03호선") || lineNum.equals("04호선") || lineNum.equals("05호선") || lineNum.equals("06호선") || lineNum.equals("07호선") || lineNum.equals("08호선")) {
                    String stationSchduleJson = getStationSchduleAPI(stationCode, week, inout);
                    // log.info("역 시간표 : " + stationSchduleJson);
                    setStationScheduleSave(stationSchduleJson, lineNum);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /* 열차 시간표 저장 */
    public void setStationScheduleSave(String json, String line) {
        try {
            JSONParser jsonParser = new JSONParser();
            JSONObject jsonObject = (JSONObject) jsonParser.parse(json);
            //log.info("jsonObject : " + jsonObject);
            JSONObject result = (JSONObject) jsonObject.get("RESULT");
            if(result != null) {
                return;
            }
            JSONObject searchSTNTimeTableByIDService = (JSONObject) jsonObject.get("SearchSTNTimeTableByIDService");
            JSONArray jsonArr = (JSONArray) searchSTNTimeTableByIDService.get("row");
            List<StationScheduleDto> stationScheduleDtoList = new ArrayList<>();
            for (int count = 0; count < jsonArr.size(); count++) {
                JSONObject row = (JSONObject) jsonArr.get(count);
                String week = (String) row.get("WEEK_TAG");
                String upDown = (String) row.get("INOUT_TAG");
                String express = (String) row.get("EXPRESS_YN");
                if(week.equals("1")) {
                    week = "평일";
                }else if (week.equals("2")) {
                    week = "토요일";
                }else {
                    week = "휴일/일요일";
                }
                upDown = upDown.equals("1") ? "상행, 내선" : "하행, 외선";
                express = express.equals("G") ? "일반" : "급행";
                String station = (String) row.get("STATION_NM");
                String stnLine = line;
                switch(stnLine) {
                    case "01호선":
                        if(station.equals("소요산") || station.equals("동두천") || station.equals("보산") || station.equals("동두천중앙") || station.equals("지행") || station.equals("덕정") || station.equals("덕계") || station.equals("양주") || station.equals("녹양") || station.equals("가능") || station.equals("의정부") || station.equals("회룡") || station.equals("망월사") || station.equals("도봉산") || station.equals("도봉") || station.equals("방학") || station.equals("창동") || station.equals("녹천") || station.equals("월계") || station.equals("광운대") || station.equals("석계") || station.equals("신이문") || station.equals("외대앞") || station.equals("회기") ||
                                station.equals("청량리") || station.equals("제기동") || station.equals("신설동") || station.equals("동묘앞") || station.equals("동대문") || station.equals("종로5가") || station.equals("종로3가") || station.equals("종각") || station.equals("시청") || station.equals("서울역") || station.equals("남영") || station.equals("용산") || station.equals("노량진") || station.equals("대방") || station.equals("신길") || station.equals("영등포") || station.equals("신도림") || station.equals("구로") || station.equals("구일") || station.equals("개봉") || station.equals("오류동") || station.equals("온수") || station.equals("역곡") || station.equals("소사") ||
                                station.equals("부천") || station.equals("중동") || station.equals("송내") || station.equals("부개") || station.equals("부평") || station.equals("백운") || station.equals("동암") || station.equals("간석") || station.equals("주안") || station.equals("도화") || station.equals("제물포") || station.equals("도원") || station.equals("동인천") || station.equals("인천")) {
                            stnLine = "경인1호선";
                        }
                        if(station.equals("가산디지털단지") || station.equals("독산") || station.equals("금천구청") || station.equals("석수") || station.equals("관악") || station.equals("안양") || station.equals("명학") || station.equals("금정") || station.equals("군포") || station.equals("당정") || station.equals("의왕") || station.equals("성균관대") || station.equals("화서") || station.equals("수원") || station.equals("세류") || station.equals("병점") || station.equals("세마") || station.equals("오산대") || station.equals("오산") || station.equals("진위") || station.equals("송탄") || station.equals("서정리") || station.equals("평택지제") || station.equals("평택") || station.equals("성환") || station.equals("직산") || station.equals("두정") || station.equals("천안") || station.equals("봉명") || station.equals("쌍용") || station.equals("아산") || station.equals("탕정") || station.equals("배방") || station.equals("온양온천") || station.equals("신창")){
                            stnLine = "경부1호선";
                        }
                        if(station.equals("서동탄")) {
                            stnLine = "병점기지선";
                        }
                        break;
                    case "02호선":
                        stnLine = "서울2호선";
                        if(station.equals("용답") || station.equals("신답") || station.equals("용두") || station.equals("신설동")) {
                            stnLine ="성수지선";
                        }
                        if(station.equals("도림천") || station.equals("양천구청") || station.equals("신정네거리")) {
                            stnLine ="신정지선";
                        }
                        break;
                    case "03호선":
                        stnLine = "수도권3호선";
                        break;
                    case "04호선":
                        stnLine = "수도권4호선";
                        break;
                    case "05호선":
                        stnLine = "수도권5호선";

                        if(station.equals("둔촌동") || station.equals("올림픽공원") || station.equals("방이") || station.equals("오금") || station.equals("개롱") || station.equals("거여") || station.equals("마천")) {
                            stnLine = "마천지선";
                        }
                        break;
                    case "06호선":
                        stnLine = "서울6호선";
                        break;
                    case "07호선":
                        stnLine = "서울7호선";
                        break;
                    case "08호선":
                        stnLine = "서울8호선";
                        break;
                    default:
                        break;
                }

                StationScheduleDto stationScheduleDto = StationScheduleDto.builder()
                        .station(station)
                        .line(stnLine)
                        .arrivalTime((String) row.get("LEFTTIME"))
                        .departureTime((String) row.get("ARRIVETIME"))
                        .arrivalStation((String) row.get("SUBWAYSNAME"))
                        .departureStation((String) row.get("SUBWAYENAME"))
                        .week(week)
                        .upDown(upDown)
                        .express(express)
                        .build();
                stationScheduleDtoList.add(stationScheduleDto);
                if(station.equals("구로") || station.equals("금정") || station.equals("병점") || station.equals("성수") || station.equals("신도림") || station.equals("까치산")) {
                    if(station.equals("구로") && stnLine.equals("01호선")) {
                        stnLine = "경인1호선";
                    }else if(station.equals("금정") && stnLine.equals("01호선")) {
                        stnLine = "수도권4호선";
                    } else if(station.equals("병점") && stnLine.equals("01호선")) {
                        stnLine = "병점기지선";
                    } else if(station.equals("성수") && stnLine.equals("서울2호선")) {
                        stnLine = "성수지선";
                    } else if(station.equals("신도림") && stnLine.equals("서울2호선")) {
                        stnLine = "신정지선";
                    } else if(station.equals("까치산") && stnLine.equals("수도권5호선")) {
                        stnLine = "신정지선";
                    }
                    stationScheduleDto = StationScheduleDto.builder()
                            .station(station)
                            .line(stnLine)
                            .arrivalTime((String) row.get("LEFTTIME"))
                            .departureTime((String) row.get("ARRIVETIME"))
                            .arrivalStation((String) row.get("SUBWAYSNAME"))
                            .departureStation((String) row.get("SUBWAYENAME"))
                            .week(week)
                            .upDown(upDown)
                            .express(express)
                            .build();
                    stationScheduleDtoList.add(stationScheduleDto);
                }
            }
            StationScheduleDto stationScheduleDto = new StationScheduleDto();
            List<StationSchedule> stationScheduleList = stationScheduleDto.toEntityList(stationScheduleDtoList);
            for (StationSchedule stationSchedule : stationScheduleList) {
                stationScheduleRepository.save(stationSchedule);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //혼잡도데이터가 담겨있는 Entity에서 겹치지 않는(distinct) 역이름을 list에 담아오기 위한 메소드
    public List<String> stationDistinctList() {
        List<String> stationList = capitalareaStationRepository.findDistinctStation();
        return stationList;
    }

    // 괄호 제거
    public String deleteBracket(String text) {
        Matcher matcher = PATTERN_BRACKET.matcher(text);
        String pureText = text;
        String removeTextArea = new String();
        while(matcher.find()) {
            int startIndex = matcher.start();
            int endIndex = matcher.end();
            removeTextArea = pureText.substring(startIndex, endIndex);
            pureText = pureText.replace(removeTextArea, VOID);
            matcher = PATTERN_BRACKET.matcher(pureText);
        }
        return pureText;
    }
}