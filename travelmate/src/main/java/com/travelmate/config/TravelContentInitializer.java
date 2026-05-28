package com.travelmate.config;

import com.travelmate.entity.TravelDestination;
import com.travelmate.entity.TravelPost;
import com.travelmate.repository.TravelDestinationRepository;
import com.travelmate.repository.TravelPostRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class TravelContentInitializer {

    @Bean
    public CommandLineRunner initTravelContent(TravelDestinationRepository destinationRepository,
                                               TravelPostRepository travelPostRepository) {
        return args -> {
            initDestinations(destinationRepository);
            initTravelPosts(travelPostRepository);
        };
    }

    private void initDestinations(TravelDestinationRepository repository) {
        List<DestinationData> items = List.of(
                new DestinationData("Đà Lạt", "da-lat", TravelDestination.Region.CENTRAL,
                        "/assets/images/homestay-o-da-lat.jpg", "Thành phố ngàn hoa, khí hậu mát mẻ quanh năm.", 1),
                new DestinationData("Nha Trang", "nha-trang", TravelDestination.Region.CENTRAL,
                        "/assets/images/MienTrung/NhaTrang.jpg", "Thành phố biển nổi tiếng với resort và đảo đẹp.", 2),
                new DestinationData("Đà Nẵng", "da-nang", TravelDestination.Region.CENTRAL,
                        "/assets/images/MienTrung/DN.jpg", "Thành phố biển hiện đại, gần Hội An và Huế.", 3),
                new DestinationData("Hội An", "hoi-an", TravelDestination.Region.CENTRAL,
                        "/assets/images/MienTrung/Hue.jpg", "Phố cổ, đèn lồng và văn hóa miền Trung.", 4),
                new DestinationData("Sa Pa", "sa-pa", TravelDestination.Region.NORTH,
                        "/assets/images/MienBac/Sapa.jpg", "Thị trấn vùng cao nổi tiếng với ruộng bậc thang.", 5),
                new DestinationData("Lào Cai", "lao-cai", TravelDestination.Region.NORTH,
                        "/assets/images/MienBac/LC.jpg", "Cửa ngõ vùng Tây Bắc, gắn với Sa Pa và Fansipan.", 6),
                new DestinationData("Hà Giang", "ha-giang", TravelDestination.Region.NORTH,
                        "/assets/images/MienBac/MB_HG.jpg", "Cung đường núi đá và trải nghiệm phượt miền Bắc.", 7),
                new DestinationData("Ninh Bình", "ninh-binh", TravelDestination.Region.NORTH,
                        "/assets/images/MienBac/NB.jpg", "Tràng An, Tam Cốc và cảnh quan núi đá vôi.", 8),
                new DestinationData("Hà Nội", "ha-noi", TravelDestination.Region.NORTH,
                        "/assets/images/MienBac/MB_HN.jpg", "Thủ đô nghìn năm văn hiến.", 9),
                new DestinationData("Quảng Ninh", "quang-ninh", TravelDestination.Region.NORTH,
                        "/assets/images/MienBac/QN.jpg", "Hạ Long, Bái Tử Long và các trải nghiệm biển đảo miền Bắc.", 10),
                new DestinationData("Yên Bái", "yen-bai", TravelDestination.Region.NORTH,
                        "/assets/images/MienBac/YB.jpg", "Ruộng bậc thang, hồ Thác Bà và cảnh sắc Tây Bắc.", 11),
                new DestinationData("Sơn La", "son-la", TravelDestination.Region.NORTH,
                        "/assets/images/MienBac/SL.jpg", "Mộc Châu, Tà Xùa và cao nguyên xanh mát.", 12),
                new DestinationData("Phú Quốc", "phu-quoc", TravelDestination.Region.SOUTH,
                        "/assets/images/MienNam/VT.jpg", "Đảo nghỉ dưỡng biển phía Nam.", 13),
                new DestinationData("TP. Hồ Chí Minh", "ho-chi-minh", TravelDestination.Region.SOUTH,
                        "/assets/images/MienNam/HCM.jpg", "Trung tâm đô thị, mua sắm và ẩm thực sôi động.", 14)
        );

        for (DestinationData item : items) {
            TravelDestination destination = repository.findBySlug(item.slug())
                    .orElseGet(TravelDestination::new);
            destination.setName(item.name());
            destination.setSlug(item.slug());
            destination.setRegion(item.region());
            destination.setImageUrl(item.imageUrl());
            destination.setShortDescription(item.description());
            destination.setActive(true);
            destination.setDisplayOrder(item.order());
            repository.save(destination);
        }
    }

    private void initTravelPosts(TravelPostRepository repository) {
        List<PostData> items = List.of(
                post("Cẩm nang du lịch Đà Lạt cho chuyến nghỉ dưỡng ngắn ngày", "Đà Lạt", "da-lat",
                        "Gợi ý tổng quan về thời tiết, di chuyển và trải nghiệm phù hợp khi khách tìm nơi lưu trú tại Đà Lạt.",
                        "Đà Lạt phù hợp với khách muốn nghỉ dưỡng trong không khí mát mẻ, kết hợp tham quan hồ, đồi thông, vườn hoa và các quán cà phê địa phương.",
                        "/assets/images/travel-posts/da-lat-guide.jpg",
                        "https://vietnam.travel/places-to-go/central-vietnam/dalat", TravelPost.Category.GUIDE),
                post("8 trải nghiệm nên thử khi đến Đà Lạt", "Đà Lạt", "da-lat",
                        "Các hoạt động nổi bật như khám phá thiên nhiên, thưởng thức cà phê và trải nghiệm khí hậu cao nguyên.",
                        "Bài gợi ý này giúp người dùng có thêm ý tưởng hoạt động sau khi đã tìm được nơi lưu trú tại Đà Lạt.",
                        "/assets/images/travel-posts/da-lat-8-things.jpg",
                        "https://vietnam.travel/things-to-do/8-things-to-do-in-dalat", TravelPost.Category.ATTRACTION),
                post("Cẩm nang du lịch Nha Trang cho kỳ nghỉ biển", "Nha Trang", "nha-trang",
                        "Thông tin tổng quan cho khách muốn nghỉ dưỡng biển, tham quan thành phố và chọn resort tại Nha Trang.",
                        "Nha Trang phù hợp với nhóm khách muốn kết hợp tắm biển, nghỉ dưỡng, ăn hải sản và tham quan các điểm văn hóa trong thành phố.",
                        "/assets/images/travel-posts/nha-trang-guide.jpg",
                        "https://vietnam.travel/places-to-go/central-vietnam/nha-trang", TravelPost.Category.GUIDE),
                post("Gợi ý trải nghiệm đảo quanh Nha Trang", "Nha Trang", "nha-trang",
                        "Gợi ý các hoạt động biển đảo phù hợp với khách đặt resort hoặc villa ở khu vực Nha Trang.",
                        "Khi khách chọn lưu trú tại Nha Trang, nhóm trải nghiệm đảo là nội dung dễ liên kết với nhu cầu nghỉ dưỡng biển.",
                        "/assets/images/travel-posts/nha-trang-island-hopping.jpg",
                        "https://vietnam.travel/things-to-do/where-to-go-when-island-hopping-around-nha-trang", TravelPost.Category.ATTRACTION),
                post("Cẩm nang khám phá phố cổ Hội An", "Hội An", "hoi-an",
                        "Tóm tắt trải nghiệm phố cổ, văn hóa địa phương, ẩm thực và các hoạt động nhẹ nhàng quanh khu lưu trú tại Hội An.",
                        "Hội An phù hợp cho khách thích phố cổ, ẩm thực địa phương, đạp xe và dạo bộ buổi tối.",
                        "/assets/images/travel-posts/hoi-an-guide.jpg",
                        "https://vietnam.travel/places-to-go/central-vietnam/hoi-an", TravelPost.Category.GUIDE),
                post("10 hoạt động nên thử ở Hội An", "Hội An", "hoi-an",
                        "Các hoạt động tham khảo cho khách lưu trú ở Hội An như đi phố cổ, thưởng thức ẩm thực, đạp xe và ghé biển gần đó.",
                        "Bài viết này gợi ý các hoạt động nhẹ nhàng, phù hợp khi khách chọn homestay hoặc cơ sở lưu trú quanh Hội An.",
                        "/assets/images/travel-posts/hoi-an-ancient-town.jpg",
                        "https://vietnam.travel/things-to-do/the-best-ways-to-explore-the-ancient-town-of-hoi-an", TravelPost.Category.ATTRACTION),
                post("Cẩm nang du lịch Đà Nẵng cho người mới đi lần đầu", "Đà Nẵng", "da-nang",
                        "Thông tin tổng quan về thành phố biển, khu vực lưu trú, trải nghiệm tham quan và nghỉ dưỡng tại Đà Nẵng.",
                        "Đà Nẵng có lợi thế kết hợp biển, trung tâm thành phố và các điểm vui chơi lân cận.",
                        "/assets/images/travel-posts/da-nang-guide.jpg",
                        "https://vietnam.travel/places-to-go/central-vietnam/da-nang", TravelPost.Category.GUIDE),
                post("Gợi ý lịch trình 3 ngày tại Đà Nẵng", "Đà Nẵng", "da-nang",
                        "Lịch trình tham khảo giúp khách sắp xếp thời gian giữa nghỉ dưỡng, ăn uống và tham quan khi lưu trú ở Đà Nẵng.",
                        "Nội dung này phù hợp sau khi khách đã chọn ngày nhận, trả phòng và muốn lên lịch trình ngắn ngày.",
                        "/assets/images/travel-posts/da-nang-itinerary.jpg",
                        "https://vietnam.travel/things-to-do/da-nang-itinerary", TravelPost.Category.ESSENTIAL),
                post("Cẩm nang du lịch Sa Pa", "Sa Pa", "sa-pa",
                        "Gợi ý tổng quan về khí hậu vùng núi, ruộng bậc thang, trekking và các trải nghiệm phù hợp tại Sa Pa.",
                        "Sa Pa là điểm đến phù hợp với khách thích cảnh núi, văn hóa địa phương và lịch trình khám phá thiên nhiên.",
                        "/assets/images/travel-posts/sa-pa-guide.jpg",
                        "https://vietnam.travel/places-to-go/northern-vietnam/sapa", TravelPost.Category.GUIDE),
                post("Khám phá Fansipan khi đến Sa Pa", "Sa Pa", "sa-pa",
                        "Gợi ý trải nghiệm Fansipan cho khách muốn thêm hoạt động nổi bật vào chuyến đi Sa Pa.",
                        "Bài viết giúp người dùng có thêm ý tưởng tham quan khi tìm kiếm nơi lưu trú tại Sa Pa.",
                        "/assets/images/travel-posts/fansipan-sapa.jpg",
                        "https://vietnam.travel/things-to-do/why-fansipan-must-do-sapa", TravelPost.Category.ATTRACTION),
                post("Cẩm nang du lịch Phú Quốc", "Phú Quốc", "phu-quoc",
                        "Thông tin tổng quan cho khách muốn nghỉ dưỡng biển đảo, chọn resort và khám phá thiên nhiên tại Phú Quốc.",
                        "Phú Quốc phù hợp với khách tìm kỳ nghỉ biển, resort, ẩm thực và trải nghiệm thiên nhiên.",
                        "/assets/images/travel-posts/phu-quoc-guide.jpg",
                        "https://vietnam.travel/places-to-go/southern-vietnam/phu-quoc", TravelPost.Category.GUIDE),
                post("Gợi ý lịch trình Phú Quốc 3 ngày 2 đêm", "Phú Quốc", "phu-quoc",
                        "Lịch trình tham khảo cho khách muốn kết hợp nghỉ dưỡng, tham quan và trải nghiệm đảo trong chuyến đi ngắn ngày.",
                        "Bài viết này phù hợp với luồng sau đặt phòng: khách đã thanh toán thành công có thể bấm xem gợi ý du lịch tại điểm đến.",
                        "/assets/images/travel-posts/phu-quoc-3-days.jpg",
                        "https://vietnam.travel/things-to-do/explore-phu-quoc-island-3-days-2-nights", TravelPost.Category.ESSENTIAL),
                post("Cẩm nang du lịch Hà Nội cho chuyến đi đầu tiên", "Hà Nội", "ha-noi",
                        "Gợi ý tổng quan về phố cổ, ẩm thực, di chuyển và các khu vực phù hợp khi khách tìm nơi lưu trú tại Hà Nội.",
                        "Hà Nội phù hợp với khách muốn kết hợp tham quan văn hóa, ẩm thực đường phố và các trải nghiệm đô thị cổ.",
                        "/assets/images/travel-posts/ha-noi-guide.jpg",
                        "https://vietnam.travel/places-to-go/northern-vietnam/ha-noi", TravelPost.Category.GUIDE),
                post("Cẩm nang du lịch Ninh Bình", "Ninh Bình", "ninh-binh",
                        "Tóm tắt trải nghiệm Tràng An, Tam Cốc, Hang Múa và các điểm tham quan núi đá vôi gần khu lưu trú.",
                        "Ninh Bình là điểm đến phù hợp với khách muốn nghỉ ngắn ngày, đi thuyền, leo núi nhẹ và khám phá cảnh quan tự nhiên.",
                        "/assets/images/travel-posts/ninh-binh-guide.jpg",
                        "https://vietnam.travel/places-to-go/northern-vietnam/ninh-binh", TravelPost.Category.GUIDE),
                post("Cẩm nang du lịch Hà Giang", "Hà Giang", "ha-giang",
                        "Gợi ý cung đường, mùa đi đẹp và những trải nghiệm nổi bật khi khách tìm nơi lưu trú hoặc gợi ý tại Hà Giang.",
                        "Hà Giang phù hợp với khách thích cảnh núi, cung đường đèo và văn hóa bản địa.",
                        "/assets/images/travel-posts/ha-giang-guide.jpg",
                        "https://vietnam.travel/places-to-go/northern-vietnam/ha-giang", TravelPost.Category.GUIDE),
                post("Gợi ý khám phá TP. Hồ Chí Minh", "TP. Hồ Chí Minh", "ho-chi-minh",
                        "Tóm tắt các trải nghiệm đô thị, ẩm thực, mua sắm và tham quan khi người dùng nhập TP.HCM, HCM, Sài Gòn hoặc Hồ Chí Minh.",
                        "TP. Hồ Chí Minh là điểm đến có nhiều cách gọi trong thực tế như TP.HCM, HCM, Sài Gòn hoặc Hồ Chí Minh.",
                        "/assets/images/travel-posts/ho-chi-minh-city-guide.jpg",
                        "https://vietnam.travel/places-to-go/southern-vietnam/ho-chi-minh-city", TravelPost.Category.GUIDE),
                post("Khám phá Vũng Tàu trong 2 ngày 1 đêm", "Vũng Tàu", "vung-tau",
                        "Gợi ý lịch trình cuối tuần đến thành phố biển gần Sài Gòn nhất, kết hợp nghỉ dưỡng và hải sản.",
                        "Vũng Tàu phù hợp cho chuyến đi ngắn ngày, tắm biển, tham quan Hải Đăng và thưởng thức hải sản ven biển.",
                        "/assets/images/travel-posts/vung-tau-guide.jpg",
                        "https://vietnam.travel/things-to-do/essential-vung-tau-guide", TravelPost.Category.GUIDE),
                post("Trải nghiệm chợ nổi Cái Răng — Cần Thơ", "Cần Thơ", "can-tho",
                        "Hướng dẫn khám phá chợ nổi nổi tiếng và ẩm thực đặc trưng miền Tây sông nước.",
                        "Cần Thơ là thủ phủ miền Tây; chợ nổi Cái Răng và ẩm thực sông nước là điểm nhấn dễ kết hợp với lịch trình lưu trú.",
                        "/assets/images/travel-posts/can-tho-guide.jpg",
                        "https://vietnam.travel/places-to-go/southern-vietnam/can-tho", TravelPost.Category.ATTRACTION),
                post("Mũi Né — thiên đường kite surf và đồi cát", "Mũi Né", "mui-ne",
                        "Điểm đến biển nổi tiếng cho thể thao nước và kỳ nghỉ biển khác biệt.",
                        "Mũi Né nổi bật với đồi cát, làng chài, resort ven biển và điều kiện gió phù hợp cho kitesurfing.",
                        "/assets/images/travel-posts/mui-ne-must-do.jpg",
                        "https://vietnam.travel/node/708", TravelPost.Category.GUIDE),
                post("Sài Gòn về đêm — lịch trình ăn uống và khám phá", "TP. Hồ Chí Minh", "ho-chi-minh",
                        "Những điểm ăn ngon và vui chơi về đêm tại thành phố năng động nhất Việt Nam.",
                        "TP.HCM về đêm phù hợp với phố đi bộ, rooftop bar, ẩm thực đường phố và lịch trình ngắn sau giờ check-in.",
                        "/assets/images/travel-posts/ho-chi-minh-nightlife.jpg",
                        "https://vietnam.travel/things-to-do/best-nightlife-ho-chi-minh-city", TravelPost.Category.ESSENTIAL),
                post("Cẩm nang Quảng Ninh và vịnh Hạ Long", "Quảng Ninh", "quang-ninh",
                        "Gợi ý tham quan Hạ Long, trải nghiệm biển đảo và lịch trình phù hợp khi khách tìm Quảng Ninh hoặc Hạ Long.",
                        "Quảng Ninh thường được người dùng tìm bằng nhiều cách như Quảng Ninh, quangninh hoặc Hạ Long.",
                        "/assets/images/travel-posts/ha-long-guide.jpg",
                        "https://vietnam.travel/places-to-go/northern-vietnam/ha-long", TravelPost.Category.GUIDE),
                post("Hồ Thác Bà và gợi ý du lịch Yên Bái", "Yên Bái", "yen-bai",
                        "Gợi ý một điểm đến thiên nhiên tại Yên Bái, phù hợp khi khách tìm yen bai hoặc yenbai.",
                        "Yên Bái phù hợp với khách muốn khám phá ruộng bậc thang, hồ Thác Bà và các cung đường Tây Bắc.",
                        "/assets/images/travel-posts/yen-bai-thac-ba.jpg",
                        "https://vietnam.travel/things-to-do/thac-ba-lake-emerald-yen-bai", TravelPost.Category.ATTRACTION),
                post("Mộc Châu - điểm đến thiên nhiên tại Sơn La", "Sơn La", "son-la",
                        "Gợi ý cao nguyên Mộc Châu, khí hậu mát mẻ và các trải nghiệm xanh khi khách tìm Sơn La hoặc sonla.",
                        "Sơn La phù hợp với khách muốn khám phá cao nguyên, khí hậu mát mẻ và các điểm đến thiên nhiên.",
                        "/assets/images/travel-posts/son-la-moc-chau.jpg",
                        "https://vietnam.travel/things-to-do/moc-chau-your-one-stop-nature-escape", TravelPost.Category.ATTRACTION),
                post("Lào Cai và hành trình lên Sa Pa", "Lào Cai", "lao-cai",
                        "Gợi ý cách nhìn Lào Cai như cửa ngõ đến Sa Pa, phù hợp cho các keyword lao cai, Lào Cai, Sa Pa và sapa.",
                        "Lào Cai thường gắn với hành trình đi Sa Pa; bài viết giúp user nhập Lào Cai vẫn có gợi ý liên quan.",
                        "/assets/images/travel-posts/lao-cai-topas-ecolodge.jpg",
                        "https://vietnam.travel/things-to-do/topas-ecolodge", TravelPost.Category.ESSENTIAL)
        );

        for (PostData item : items) {
            TravelPost post = repository.findFirstBySourceUrlOrderByIdAsc(item.sourceUrl())
                    .or(() -> repository.findFirstByTitleOrderByIdAsc(item.title()))
                    .orElseGet(TravelPost::new);
            post.setTitle(item.title());
            post.setDestination(item.destinationName());
            post.setDestinationSlug(item.destinationSlug());
            post.setSummary(item.summary());
            post.setContent(item.content() + "\n\nTravelMate lưu bản tóm tắt tự biên soạn và dẫn link nguồn thật để người dùng đọc thêm.");
            post.setThumbnailUrl(item.thumbnailUrl());
            post.setSourceName("Vietnam.travel");
            post.setSourceUrl(item.sourceUrl());
            post.setCategory(item.category());
            post.setStatus(TravelPost.Status.VISIBLE);
            post.setCreatedBy("admin@travelmate.vn");
            repository.save(post);
        }
    }

    private static PostData post(String title, String destinationName, String destinationSlug,
                                 String summary, String content, String thumbnailUrl,
                                 String sourceUrl, TravelPost.Category category) {
        return new PostData(title, destinationName, destinationSlug, summary, content,
                thumbnailUrl, sourceUrl, category);
    }

    private record DestinationData(String name, String slug, TravelDestination.Region region,
                                   String imageUrl, String description, int order) {
    }

    private record PostData(String title, String destinationName, String destinationSlug,
                            String summary, String content, String thumbnailUrl,
                            String sourceUrl, TravelPost.Category category) {
    }
}
