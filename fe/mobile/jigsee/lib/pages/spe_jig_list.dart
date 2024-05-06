import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:jigsee/api/dio_instance.dart';
import 'package:jigsee/api/provider.dart';
import 'package:jigsee/components/header.dart';
import 'package:jigsee/consts/size.dart';

class SpeJigList extends ConsumerWidget {
  const SpeJigList({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final equipmentName = ref.watch(equipmentProvider)[0];
    final DioClient dioClient = ref.read(dioClientProvider);

    Future<List<String>> fetchEquipmentDetails() async {
      try {
        var response = await dioClient.get(
            '/equipment/details',
            queryParameters: {'equipmentName': equipmentName}
        );
        List<dynamic> data = response.data['details'];
        return data.map<String>((e) => 'S/N: ${e['serialNumber']}').toList();
      } catch (e) {
        return [
          'A0001234',
          'A0001234',
          'A0001234',
          'A0001234',
          'A0001234',
          'A0001234',
          'A0001234',
          'A0001234',
        ];
      }
    }

    return Scaffold(
      appBar: const CustomAppBar(),
      body: Column(
        children: [
          const SizedBox(height: largeGap),
          const Text('교체 지그 리스트', style: TextStyle(
            fontSize: 24,
            fontWeight: FontWeight.bold,
            ),
          ),
          const SizedBox(height: largeGap),
          Text(equipmentName, style: const TextStyle(
              fontSize: 20,
              fontWeight: FontWeight.bold,
            ),
          ),
          const SizedBox(height: largeGap),
          Expanded(
            child: Container(
              padding: const EdgeInsets.only(left: 12, right: 12),
              child: FutureBuilder<List<String>>(
                future: fetchEquipmentDetails(),
                builder: (context, snapshot) {
                  if (snapshot.connectionState == ConnectionState.done) {
                    if (snapshot.hasError) {
                      return const Text('불러 오기 실패');
                    }
                    List<String> details = snapshot.data ?? [];
                    return ListView.builder(
                      itemCount: details.length,
                      itemBuilder: (context, index) => Card(
                        child: ListTile(
                          title: Text(details[index]),
                          trailing: const Icon(Icons.arrow_forward_ios),
                          onTap: () {
                            ref.read(selectedJigProvider.notifier).state = details[index];
                            Navigator.pushNamed(context, '/ocr');
                          },
                        ),
                      ),
                    );
                  } else {
                    return const Center(child: CircularProgressIndicator());
                  }
                },
              ),
            )
          ),
        ],
      )
    );
  }
}