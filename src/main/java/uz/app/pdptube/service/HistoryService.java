package uz.app.pdptube.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.app.pdptube.entity.History;
import uz.app.pdptube.entity.HistoryVideos;
import uz.app.pdptube.entity.User;
import uz.app.pdptube.entity.Video;
import uz.app.pdptube.helper.Helper;
import uz.app.pdptube.payload.ResponseMessage;
import uz.app.pdptube.repository.HistoryRepository;
import uz.app.pdptube.repository.HistoryVideosRepository;
import uz.app.pdptube.repository.VideoRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class HistoryService {
    private final HistoryRepository historyRepository;
    private final HistoryVideosRepository historyVideosRepository;
    private final VideoRepository videoRepository;

    public ResponseMessage getHistory() {
        User principal = Helper.getCurrentPrincipal();
        Optional<History> optionalHistory = historyRepository.findByOwner(principal);

        if (optionalHistory.isEmpty()) {
            return new ResponseMessage(false, "you haven't watched a video so far", null);
        }

        History history = optionalHistory.get();
        List<HistoryVideos> historyVideos = historyVideosRepository.findAllByHistory(history.getId());
        List<Video> videosList = new ArrayList<>();

        for (HistoryVideos historyVideo : historyVideos) {
            videoRepository.findById(historyVideo.getVideo())
                    .ifPresent(videosList::add); // video mavjud bo‘lsa qo‘shamiz, yo‘q bo‘lsa skip
        }

        return new ResponseMessage(true, "here are all the videos in your history", videosList);
    }


    @Transactional
    public ResponseMessage refreshHistory(){
        User principal = Helper.getCurrentPrincipal();
        Optional<History> optionalHistory = historyRepository.findByOwner(principal);
        if (optionalHistory.isPresent()) {
            History history = optionalHistory.get();
            historyVideosRepository.deleteAllByHistory(history.getId());
            return new ResponseMessage(true, "history refreshed", historyVideosRepository.findAllByHistory(history.getId()));
        }else {
            return new ResponseMessage(false, "you don't have any videos in your history", optionalHistory.get());
        }
    }



}
