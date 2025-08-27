package uz.app.pdptube.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.app.pdptube.dto.ChannelDTO;
import uz.app.pdptube.entity.Channel;
import uz.app.pdptube.entity.ChannelOwner;
import uz.app.pdptube.entity.Subscription;
import uz.app.pdptube.entity.User;
import uz.app.pdptube.helper.Helper;
import uz.app.pdptube.payload.ResponseMessage;
import uz.app.pdptube.repository.*;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChannelService {
    private final ChannelRepository channelRepository;
    private final ChannelOwnerRepository channelOwnerRepository;
    private final VideoRepository videoRepository;
    private final SubscriptionRepository subscriptionRepository;

    public ResponseMessage getChannel(){
        User principal = Helper.getCurrentPrincipal();
        Optional<ChannelOwner> optionalChannelOwner = channelOwnerRepository.findByOwner(principal.getId());

        return optionalChannelOwner
                .flatMap(co -> channelRepository.findById(co.getChannel()))
                .map(ch -> new ResponseMessage(true, "Here is the channel", ch))
                .orElseGet(() ->
                        new ResponseMessage(false, "you don't have channel, but you can create it", principal)
                );
    }

    @Transactional
    public ResponseMessage createChannel(ChannelDTO channelDTO){
        Optional<ChannelOwner> relation = channelOwnerRepository.findByOwner(Helper.getCurrentPrincipal().getId());
        if (relation.isPresent()) {
            return channelRepository.findById(relation.get().getChannel())
                    .map(ch -> new ResponseMessage(false, "you already have a channel", ch))
                    .orElseGet(() -> new ResponseMessage(false, "you already have a channel", relation.get()));
        } else {
            Channel newChannel = Channel.builder()
                    .description(channelDTO.getDescription())
                    .profilePicture(channelDTO.getProfilePicture())
                    .name(channelDTO.getName())
                    .build();
            channelRepository.save(newChannel);

            ChannelOwner channelOwner = new ChannelOwner();
            channelOwner.setOwner(Helper.getCurrentPrincipal().getId());
            channelOwner.setChannel(newChannel.getId());
            channelOwnerRepository.save(channelOwner);

            return new ResponseMessage(true, "Your channel has been created", newChannel);
        }
    }

    @Transactional
    public ResponseMessage deleteChannel() {
        User principal = Helper.getCurrentPrincipal();
        Optional<ChannelOwner> relation = channelOwnerRepository.findByOwner(principal.getId());

        if (relation.isPresent()) {
            ChannelOwner channelOwner = relation.get();
            Optional<Channel> optionalChannel = channelRepository.findById(channelOwner.getChannel());

            if (optionalChannel.isPresent()) {
                Channel channel = optionalChannel.get();
                deleteAllVideosByChannelId(channel);
                channelRepository.delete(channel);
            }

            channelOwnerRepository.delete(channelOwner);
            return new ResponseMessage(true, "Your channel has been deleted", channelOwner);

        } else {
            return new ResponseMessage(false, "you don't have a channel", principal);
        }
    }


    private void deleteAllVideosByChannelId(Channel channel) {
        videoRepository.deleteAllByChannel(channel);
    }

    public List<Integer> getAllFollowers(Integer channelId) {
        List<Subscription> subscriptions = subscriptionRepository.findAllByChannel(channelId);
        return subscriptions.stream().map(Subscription::getFollower).toList();
    }

    public ResponseMessage getFollowersNumber(Integer channelId) {
        if (channelRepository.existsById(channelId)) {
            List<Subscription> subscriptions = subscriptionRepository.findAllByChannel(channelId);
            int followers = subscriptions.size();
            return new ResponseMessage(true, "Here is the number of followers", followers);
        }else {
            return new ResponseMessage(false, "channel with this id doesnt exist", channelId);
        }

    }
}

