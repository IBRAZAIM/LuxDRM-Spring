package kz.ibrazaim.catalog.service;

import kz.ibrazaim.catalog.model.Product;
import kz.ibrazaim.catalog.model.ProductImage;
import kz.ibrazaim.catalog.repository.ProductImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ProductImageService {
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".webp", ".gif", ".avif"
    );

    private final ProductImageRepository productImageRepository;

    @Value("${app.upload.dir:uploads/products}")
    private String uploadDir;

    @Transactional
    public void saveUploadedFiles(Product product, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return;
        }
        List<String> savedPaths = storeFilesOnDisk(files);
        create(product, savedPaths);
    }

    @Transactional
    public void create(Product product, List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return;
        }
        for (String url : imageUrls) {
            if (url != null && !url.trim().isEmpty()) {
                ProductImage productImage = new ProductImage();
                productImage.setProduct(product);
                productImage.setImage(url.trim());
                ProductImage saved = productImageRepository.save(productImage);
                if (product.getImages() != null) {
                    product.getImages().add(saved);
                }
            }
        }
    }

    @Transactional
    public void replaceImages(Product product, List<String> keptImages, List<MultipartFile> newFiles) {
        List<ProductImage> currentImages = productImageRepository.findAllByProduct(product);
        Set<String> existingForProduct = new HashSet<>();
        for (ProductImage img : currentImages) {
            if (img.getImage() != null) {
                existingForProduct.add(img.getImage().trim());
            }
        }
        Set<String> keptSet = new LinkedHashSet<>();
        if (keptImages != null) {
            for (String img : keptImages) {
                if (img != null && !img.trim().isEmpty() && existingForProduct.contains(img.trim())) {
                    keptSet.add(img.trim());
                }
            }
        }

        List<String> newImagePaths = (newFiles != null && !newFiles.isEmpty())
                ? storeFilesOnDisk(newFiles)
                : List.of();

        // Удаляем с диска локальные файлы, которые были убраны при редактировании
        for (ProductImage oldImg : currentImages) {
            String path = oldImg.getImage();
            if (path != null && !keptSet.contains(path)) {
                deleteLocalFileIfUploaded(path);
            }
        }

        if (product.getImages() != null) {
            product.getImages().clear();
        }
        productImageRepository.deleteAllByProduct(product);
        productImageRepository.flush();

        List<String> allImagePaths = new ArrayList<>(keptSet);
        allImagePaths.addAll(newImagePaths);

        create(product, allImagePaths);
    }

    private List<String> storeFilesOnDisk(List<MultipartFile> files) {
        List<String> storedPaths = new ArrayList<>();
        Path targetDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(targetDir);
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось создать директорию для загрузки изображений", e);
        }

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            String originalName = file.getOriginalFilename();
            String ext = extractExtension(originalName, file.getContentType());
            if (!ALLOWED_EXTENSIONS.contains(ext)) {
                throw new IllegalArgumentException(
                        "Недопустимый формат файла: " + (originalName != null ? originalName : "") +
                        ". Разрешены: JPG, PNG, WEBP, GIF, AVIF"
                );
            }

            String filename = UUID.randomUUID() + ext;
            Path destination = targetDir.resolve(filename).normalize();
            if (!destination.startsWith(targetDir)) {
                throw new SecurityException("Некорректный путь сохранения файла");
            }

            try (InputStream in = file.getInputStream()) {
                Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
                storedPaths.add("/uploads/products/" + filename);
            } catch (IOException e) {
                throw new IllegalStateException("Ошибка при сохранении файла: " + originalName, e);
            }
        }
        return storedPaths;
    }

    private String extractExtension(String filename, String contentType) {
        if (filename != null) {
            int dotIndex = filename.lastIndexOf('.');
            if (dotIndex >= 0 && dotIndex < filename.length() - 1) {
                String ext = filename.substring(dotIndex).toLowerCase(Locale.ROOT).trim();
                if (ALLOWED_EXTENSIONS.contains(ext)) {
                    return ext;
                }
            }
        }
        if (contentType != null) {
            String ct = contentType.toLowerCase(Locale.ROOT);
            if (ct.contains("jpeg") || ct.contains("jpg")) return ".jpg";
            if (ct.contains("png")) return ".png";
            if (ct.contains("webp")) return ".webp";
            if (ct.contains("gif")) return ".gif";
            if (ct.contains("avif")) return ".avif";
        }
        return "";
    }

    public void deleteLocalFiles(Collection<String> webPaths) {
        if (webPaths == null || webPaths.isEmpty()) {
            return;
        }
        for (String path : webPaths) {
            deleteLocalFileIfUploaded(path);
        }
    }

    private void deleteLocalFileIfUploaded(String webPath) {
        if (webPath == null || !webPath.startsWith("/uploads/products/")) {
            return;
        }
        String filename = webPath.substring("/uploads/products/".length());
        if (filename.isEmpty() || filename.contains("/") || filename.contains("\\") || filename.contains("..")) {
            return;
        }
        try {
            Path targetDir = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path filePath = targetDir.resolve(filename).normalize();
            if (filePath.startsWith(targetDir)) {
                Files.deleteIfExists(filePath);
            }
        } catch (IOException ignored) {
        }
    }

    public List<ProductImage> findAllByProduct(Product product) {
        return productImageRepository.findAllByProduct(product);
    }
}
